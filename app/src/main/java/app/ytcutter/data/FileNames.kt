package app.ytcutter.data

object FileNames {

    private val ILLEGAL = setOf('/', '\\', '%', ':', '*', '?', '"', '<', '>', '|')
    private const val MAX_LENGTH = 80

    fun sanitize(title: String): String {
        var out = title.filterNot { it in ILLEGAL || it.code < 0x20 }.trim()
        if (out.length > MAX_LENGTH) {
            out = out.take(MAX_LENGTH)
            if (out.last().isHighSurrogate()) out = out.dropLast(1)
        }
        return out.ifBlank { "video" }
    }

    fun displayName(title: String, start: Int, end: Int, ext: String): String =
        "${sanitize(title)}_${start}-${end}.$ext"
}