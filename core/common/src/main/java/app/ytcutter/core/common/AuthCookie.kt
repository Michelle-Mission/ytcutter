package app.ytcutter.core.common

/** Cookie login YouTube: keberadaan salah satu nama ini berarti sesi login aktif. */
object AuthCookie {

    private val names = setOf(
        "SID",
        "__Secure-1PSID",
        "__Secure-3PSID",
        "LOGIN_INFO"
    )

    fun isLoggedIn(lines: List<String>): Boolean = lines.any { line ->
        val fields = line.trim().removePrefix("#HttpOnly_").split('\t')
        fields.size >= 6 && fields[5] in names
    }
}
