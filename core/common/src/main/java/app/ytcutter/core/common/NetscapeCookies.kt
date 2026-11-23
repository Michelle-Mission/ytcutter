package app.ytcutter.core.common

/** Parser netscape cookies.txt → header `Cookie`. Baris `#HttpOnly_` tetap dipakai. */
object NetscapeCookies {

    fun header(lines: List<String>): String? {
        val pairs = lines.asSequence()
            .map { it.removePrefix("#HttpOnly_") }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size >= 7) "${parts[5]}=${parts[6]}" else null
            }
            .joinToString("; ")
        return pairs.takeIf { it.isNotBlank() }
    }
}
