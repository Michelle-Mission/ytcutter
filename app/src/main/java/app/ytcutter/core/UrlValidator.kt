package app.ytcutter.core

object UrlValidator {

    private val allowedHosts = setOf("youtube.com", "youtu.be", "music.youtube.com")
    private val videoPaths = listOf("/watch", "/shorts/", "/live/", "/embed/")

    fun isYouTube(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return false
        if (!text.startsWith("http://") && !text.startsWith("https://")) return false
        val uri = runCatching { java.net.URI(text) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return false
        if (host !in allowedHosts) return false
        val path = uri.path.orEmpty()

        if (host == "youtu.be") {
            return path.trim('/').isNotEmpty()
        }
        if (path.startsWith("/watch")) {
            return queryParam(uri.rawQuery, "v") != null
        }
        return videoPaths.any { it != "/watch" && path.startsWith(it) && path.length > it.length }
    }

    private fun queryParam(rawQuery: String?, key: String): String? {
        if (rawQuery.isNullOrBlank()) return null
        return rawQuery.split('&')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null else part.take(idx) to part.substring(idx + 1)
            }
            .firstOrNull { it.first == key && it.second.isNotBlank() }
            ?.second
    }
}