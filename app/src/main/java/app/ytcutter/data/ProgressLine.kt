package app.ytcutter.data

data class DownloadProgress(val percent: Int, val detail: String)

/** Parser baris progress yt-dlp: template `YTC` dan format `[download]` lama. */
object ProgressLine {

    private val template = Regex("""^YTC\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)""")
    private val legacy = Regex("""\[download]\s+(\d+(?:\.\d+)?)%""")

    fun parse(line: String): DownloadProgress? {
        val text = line.trim()
        template.find(text)?.let { m ->
            val down = m.groupValues[1].toLongOrNull() ?: -1L
            val total = m.groupValues[2].toLongOrNull() ?: -1L
            val pct = m.groupValues[3].removeSuffix("%").toFloatOrNull()
            val percent = when {
                total > 0 && down >= 0 -> ((down * 100) / total).toInt()
                pct != null -> pct.toInt()
                else -> return null
            }
            val parts = listOf(m.groupValues[4], m.groupValues[5])
                .filter { it != "NA" && it.isNotBlank() }
            val detail = when (parts.size) {
                2 -> "${parts[0]} · ETA ${parts[1]}"
                1 -> outputDetailOf(m.groupValues[4], m.groupValues[5], parts[0])
                else -> ""
            }
            return DownloadProgress(percent.coerceIn(0, 100), detail)
        }
        legacy.find(text)?.let { m ->
            val percent = m.groupValues[1].toFloatOrNull() ?: return null
            return DownloadProgress(percent.toInt().coerceIn(0, 100), text.take(60))
        }
        return null
    }

    private fun outputDetailOf(speed: String, eta: String, kept: String): String =
        if (kept == eta && eta != "NA") "ETA $eta" else kept
}