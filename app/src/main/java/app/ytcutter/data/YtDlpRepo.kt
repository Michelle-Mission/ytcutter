package app.ytcutter.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object YtDlpRepo {

    const val DOWNLOAD_PROCESS_ID = "download"
    private const val PREFS = "ytdlp"
    private const val KEY_UPDATED = "initial_update_done"

    private val mutex = Mutex()

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
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
        mutex.withLock {
            runCatching { YoutubeDL.getInstance().versionName(context) }.getOrNull()
        }
    }

    suspend fun update(context: Context): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                when (YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)) {
                    YoutubeDL.UpdateStatus.DONE -> "yt-dlp diperbarui"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp sudah terbaru"
                    null -> "Gagal memperbarui yt-dlp"
                }
            }.getOrElse { "Gagal: ${it.message}" }
        }
    }

    suspend fun json(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val request = YoutubeDLRequest(url).apply {
                    addOption("-J")
                    addOption("--no-playlist")
                    addOption("--no-warnings")
                }
                val response = YoutubeDL.getInstance().execute(request)
                response.out.lineSequence().firstOrNull { it.trimStart().startsWith("{") }
            }.getOrNull()
        }
    }

    suspend fun execute(
        context: Context,
        url: String,
        options: List<Pair<String, String?>>,
        onLine: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val request = YoutubeDLRequest(url)
            options.forEach { (option, value) ->
                if (value == null) request.addOption(option) else request.addOption(option, value)
            }
            val response = YoutubeDL.getInstance().execute(
                request,
                DOWNLOAD_PROCESS_ID
            ) { _, _, line -> if (line.isNotBlank()) onLine(line) }
            response.exitCode
        }
    }

    fun cancel(): Boolean = YoutubeDL.getInstance().destroyProcessById(DOWNLOAD_PROCESS_ID)
}