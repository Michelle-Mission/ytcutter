package app.ytcutter.core

object UrlValidator {

    private val allowedHosts = setOf("youtube.com", "youtu.be", "music.youtube.com")

    fun isYouTube(raw: String): Boolean {
        val url = raw.trim()
        if (url.isEmpty() || url.any { it.isWhitespace() }) return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val host = runCatching { java.net.URI(url).host }.getOrNull() ?: return false
        val normalized = host.lowercase().removePrefix("www.").removePrefix("m.")
        return normalized in allowedHosts
    }
}