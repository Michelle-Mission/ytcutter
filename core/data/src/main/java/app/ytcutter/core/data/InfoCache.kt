package app.ytcutter.core.data

import android.content.Context
import app.ytcutter.core.common.UrlValidator
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Menyimpan JSON `yt-dlp -J` per video untuk `--load-info-json`. */
object InfoCache {

    private val paths = ConcurrentHashMap<String, String>()
    private val memory = object : LinkedHashMap<String, Pair<VideoInfo, String?>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<VideoInfo, String?>>) =
            size > 8
    }

    @Synchronized
    fun getInfo(url: String): Pair<VideoInfo, String?>? = memory[url]

    @Synchronized
    fun putInfo(url: String, info: VideoInfo, path: String?) {
        memory[url] = info to path
    }

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

    fun existingPath(dir: File, url: String): String? =
        File(dir, fileName(url)).takeIf { it.exists() && it.length() > 0 }?.absolutePath

    fun pathFor(url: String): String? {
        val path = paths[fileName(url)] ?: return null
        return path.takeIf { File(it).length() > 0 }
    }
}
