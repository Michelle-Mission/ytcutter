package app.ytcutter.core.data

import app.ytcutter.core.model.Action

data class UserError(val message: String, val action: Action)

object ErrorMapper {

    private val cookiesPatterns = listOf(
        "sign in to confirm",
        "not a bot",
        "http error 429",
        "too many requests",
        "age-restricted",
        "confirm your age",
        "login required",
        "private video",
        "this video is private",
        "members-only",
        "cookies"
    )

    private val retryPatterns = listOf(
        "requested format is not available",
        "no video formats found",
        "unable to extract"
    )

    private val cookieMessage =
        "YouTube meminta verifikasi. Buka youtube.com di browser, login, ekspor cookies.txt, " +
            "lalu impor di Pengaturan. Setelah itu coba lagi."

    fun map(raw: String): UserError {
        val text = raw.trim()
        if (text.isEmpty()) return UserError("Unduhan gagal tanpa pesan. Coba lagi.", Action.NONE)
        val lower = text.lowercase()
        if (cookiesPatterns.any { lower.contains(it) }) {
            return UserError(cookieMessage, Action.IMPORT_COOKIES)
        }
        if (retryPatterns.any { lower.contains(it) }) {
            return UserError(
                "Format itu tidak tersedia. Pilih resolusi lain, atau update yt-dlp.",
                Action.RETRY
            )
        }
        if (lower.contains("unsupported url")) {
            return UserError("URL tidak didukung yt-dlp. Pastikan tautan video YouTube.", Action.NONE)
        }
        return UserError(shorten(text), Action.NONE)
    }

    private fun shorten(text: String): String {
        val clean = text.removePrefix("ERROR:").trim()
        return if (clean.length <= 200) clean else clean.take(200) + "…"
    }
}