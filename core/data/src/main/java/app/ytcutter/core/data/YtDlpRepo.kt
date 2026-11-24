package app.ytcutter.core.data

import android.content.Context
import app.ytcutter.core.common.DeviceProfile
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface EngineState {
    data object Starting : EngineState
    data object Ready : EngineState
    data class Failed(val message: String, val detail: String? = null) : EngineState
}

data class JsonResult(val json: String?, val error: String?)

object YtDlpRepo {

    private const val PREFS = "ytdlp"
    private const val KEY_UPDATED = "initial_update_done"

    private val _state = MutableStateFlow<EngineState>(EngineState.Starting)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val initMutex = Mutex()
    private val metaMutex = Mutex()
    private val downloadMutex = Mutex()

    @Volatile
    private var ready = false

    fun newProcessId(): String = "dl-${UUID.randomUUID()}"

    suspend fun ensureInit(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (ready) return@withContext Result.success(Unit)
        initMutex.withLock {
            if (ready) return@withLock Result.success(Unit)
            _state.value = EngineState.Starting
            try {
                YoutubeDL.getInstance().init(context.applicationContext)
                runCatching { Aria2.init(context.applicationContext) }
                ready = true
                _state.value = EngineState.Ready
                Result.success(Unit)
            } catch (t: Throwable) {
                _state.value = EngineState.Failed(humanize(t), t.stackTraceToString())
                Result.failure(t)
            }
        }
    }

    private fun humanize(t: Throwable): String {
        if (t is OutOfMemoryError) {
            return "Memori perangkat tidak cukup saat menyiapkan mesin. Tutup app lain lalu coba lagi."
        }
        val chain = generateSequence(t) { it.cause }
            .take(5)
            .joinToString(" | ") { error ->
                val name = error.javaClass.simpleName
                val message = error.message?.take(300)?.takeIf { it.isNotBlank() }
                if (message == null) name else "$name: $message"
            }
        return "Gagal menyiapkan mesin — $chain"
    }

    @Volatile
    private var ffmpegReady = false

    private suspend fun ensureFfmpeg(context: Context) {
        if (ffmpegReady) return
        initMutex.withLock {
            if (ffmpegReady) return@withLock
            FFmpeg.getInstance().init(context.applicationContext)
            ffmpegReady = true
        }
    }

    suspend fun autoUpdateOnce(context: Context) {
        if (hasCheckedUpdate(context)) return
        val result = update(context)
        if (result == "yt-dlp diperbarui" || result == "yt-dlp sudah terbaru") {
            markUpdateChecked(context)
        }
    }

    fun hasCheckedUpdate(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_UPDATED, false)

    fun markUpdateChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_UPDATED, true).apply()
    }

    private suspend fun awaitReady(context: Context) {
        if (ready) return
        val result = ensureInit(context)
        result.exceptionOrNull()?.let { throw it }
    }

    suspend fun versionName(context: Context): String? = withContext(Dispatchers.IO) {
        awaitReady(context)
        initMutex.withLock {
            runCatching { YoutubeDL.getInstance().versionName(context) }.getOrNull()
        }
    }

    // ponytail: update hanya dikunci initMutex; menimpa binary saat proses lain
    // berjalan aman di Linux (inode tetap). Serialkan penuh jika muncul race nyata.
    suspend fun update(context: Context): String = withContext(Dispatchers.IO) {
        awaitReady(context)
        initMutex.withLock {
            runCatching {
                when (YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)) {
                    YoutubeDL.UpdateStatus.DONE -> "yt-dlp diperbarui"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp sudah terbaru"
                    null -> "Gagal memperbarui yt-dlp"
                }
            }.getOrElse { "Gagal: ${it.message}" }
        }
    }

    suspend fun json(context: Context, url: String): JsonResult = withContext(Dispatchers.IO) {
        awaitReady(context)
        metaMutex.withLock {
            // Info tidak butuh poToken: jalur tanpa token jauh lebih cepat.
            val first = runJson(context, url, null)
            if (first.json != null) return@withLock first
            val extractorArgs = youtubeArgs(context, url, DeviceProfile.of(context).tokenTimeoutMs)
            if (extractorArgs == null) first else runJson(context, url, extractorArgs)
        }
    }

    private suspend fun runJson(context: Context, url: String, extractorArgs: String?): JsonResult {
        return try {
            val request = YoutubeDLRequest(url).apply {
                addOption("-J")
                addOption("--no-playlist")
                addOption("--no-warnings")
                addOption("--no-check-formats")
                addOption("--socket-timeout", "15")
                addOption("--extractor-retries", "1")
                CookieGate.active(context)?.let { addOption("--cookies", it) }
                extractorArgs?.let { addOption("--extractor-args", it) }
            }
            val response = YoutubeDL.getInstance().execute(request)
            val line = response.out.lineSequence().firstOrNull { it.trimStart().startsWith("{") }
            if (line != null && response.exitCode == 0) {
                JsonResult(line, null)
            } else {
                JsonResult(null, response.err.ifBlank { "yt-dlp keluar dengan kode ${response.exitCode}" })
            }
        } catch (e: Exception) {
            JsonResult(null, e.message ?: "gagal menjalankan yt-dlp")
        }
    }

    suspend fun execute(
        context: Context,
        url: String,
        options: List<Pair<String, String?>>,
        processId: String,
        onLine: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        awaitReady(context)
        ensureFfmpeg(context)
        downloadMutex.withLock {
            val fromInfoJson = options.any { it.first == "--load-info-json" }
            val extractorArgs = if (fromInfoJson) null else youtubeArgs(context, url)
            val exit = runDownload(url, options, processId, extractorArgs, onLine)
            when {
                exit == 0 -> exit
                // JSON cache bisa kedaluwarsa: ulangi dengan ekstraksi penuh.
                fromInfoJson -> runDownload(url, DownloadArgs.stripInfoJson(options), processId, null, onLine)
                extractorArgs != null -> runDownload(url, options, processId, null, onLine)
                else -> exit
            }
        }
    }

    private suspend fun runDownload(
        url: String,
        options: List<Pair<String, String?>>,
        processId: String,
        extractorArgs: String?,
        onLine: (String) -> Unit
    ): Int {
        val request = YoutubeDLRequest(url)
        extractorArgs?.let { request.addOption("--extractor-args", it) }
        options.forEach { (option, value) ->
            if (value == null) request.addOption(option) else request.addOption(option, value)
        }
        val response = YoutubeDL.getInstance().execute(request, processId) { _, _, line ->
            if (line.isNotBlank()) onLine(line)
        }
        return response.exitCode
    }

    private suspend fun youtubeArgs(
        context: Context,
        url: String,
        timeoutMs: Long = 25_000
    ): String? {
        if (!app.ytcutter.core.common.UrlValidator.isYouTube(url)) return null
        val token = PoToken.extractorArgs(context, url, timeoutMs) ?: return null
        // tv_downgraded bikin "The page needs to be reloaded" saat pakai cookies.
        return "youtube:player_client=default,-tv_downgraded;$token"
    }

    fun cancel(processId: String?): Boolean {
        if (processId == null) return false
        return runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }.getOrDefault(false)
    }
}