package app.ytcutter.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class JsonResult(val json: String?, val error: String?)

object YtDlpRepo {

    private const val PREFS = "ytdlp"
    private const val KEY_UPDATED = "initial_update_done"

    private val initMutex = Mutex()
    private val metaMutex = Mutex()
    private val downloadMutex = Mutex()

    fun newProcessId(): String = "dl-${UUID.randomUUID()}"

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        initMutex.withLock {
            YoutubeDL.getInstance().init(context)
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_UPDATED, false)) {
                runCatching {
                    YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                    prefs.edit().putBoolean(KEY_UPDATED, true).apply()
                }
            }
        }
    }

    suspend fun versionName(context: Context): String? = withContext(Dispatchers.IO) {
        initMutex.withLock {
            runCatching { YoutubeDL.getInstance().versionName(context) }.getOrNull()
        }
    }

    suspend fun update(context: Context): String = withContext(Dispatchers.IO) {
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
        metaMutex.withLock {
            try {
                val request = YoutubeDLRequest(url).apply {
                    addOption("-J")
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                }
                val response = YoutubeDL.getInstance().execute(request)
                val line = response.out.lineSequence()
                    .firstOrNull { it.trimStart().startsWith("{") }
                if (line != null && response.exitCode == 0) {
                    JsonResult(line, null)
                } else {
                    JsonResult(null, response.err.ifBlank { "yt-dlp keluar dengan kode ${response.exitCode}" })
                }
            } catch (e: Exception) {
                JsonResult(null, e.message ?: "gagal menjalankan yt-dlp")
            }
        }
    }

    suspend fun execute(
        context: Context,
        url: String,
        options: List<Pair<String, String?>>,
        processId: String,
        onLine: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val request = YoutubeDLRequest(url)
            options.forEach { (option, value) ->
                if (value == null) request.addOption(option) else request.addOption(option, value)
            }
            val response = YoutubeDL.getInstance().execute(
                request,
                processId
            ) { _, _, line -> if (line.isNotBlank()) onLine(line) }
            response.exitCode
        }
    }

    fun cancel(processId: String?): Boolean {
        if (processId == null) return false
        return runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }.getOrDefault(false)
    }
}