package app.ytcutter.core.common

/** Gabung/parse file cookie Netscape. Baris `#HttpOnly_` adalah data, bukan komentar. */
object NetscapeFile {

    const val HEADER = "# Netscape HTTP Cookie File\n"

    fun build(contents: List<String>): String {
        val lines = linkedSetOf<String>()
        contents.forEach { content ->
            content.lines().forEach { raw ->
                val line = raw.trimEnd('\r')
                val check = line.trimStart()
                if (isCookieLine(check)) lines += line
            }
        }
        return HEADER + lines.joinToString("\n", postfix = if (lines.isEmpty()) "" else "\n")
    }

    fun parseClipboard(text: String): String? {
        val lines = text.lines()
            .map { it.trimEnd('\r') }
            .filter { isCookieLine(it.trimStart()) }
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    fun isCookieLine(trimmed: String): Boolean {
        if (trimmed.isBlank()) return false
        if (trimmed.startsWith("#HttpOnly_")) return true
        if (trimmed.startsWith("#")) return false
        return trimmed.split('\t').size >= 7
    }
}