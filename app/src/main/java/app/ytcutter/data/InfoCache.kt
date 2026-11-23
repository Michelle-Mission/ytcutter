package app.ytcutter.data

import android.content.Context
import app.ytcutter.core.UrlValidator
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Menyimpan JSON `yt-dlp -J` per video untuk `--load-info-json`. */
object InfoCache {

    private val paths = ConcurrentHashMap<String, String>()

    fun fileName(url: String): String {
        val id = UrlValidator.videoId(url) ?: url.hashCode().toUInt().toString(16)
        return "info-$id.json"
    }

    fun save(context: Context, url: String, json: String): String? = runCatching {
        val file = File(context.cacheDir, fileName(url))
        file.writeText(json)
        paths[fileName(url)] = file.absolutePath
        file.absolutePath
    }.getOrNull()

    fun pathFor(url: String): String? {
        val path = paths[fileName(url)] ?: return null
        return path.takeIf { File(it).length() > 0 }
    }
}
