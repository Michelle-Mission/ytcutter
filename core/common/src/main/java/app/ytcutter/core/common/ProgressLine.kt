package app.ytcutter.core.common

data class DownloadProgress(val percent: Int, val detail: String)

/**
 * Parser baris progress yt-dlp. Persen dihitung dari byte, lalu fragmen, lalu
 * teks `_percent_str`; bila tidak ada angka sama sekali hasilnya -1
 * (indeterminan) supaya UI tidak menampilkan 0% palsu.
 */
object ProgressLine {

    private val template = Regex(
        """^YTC\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)"""
    )
    private val legacy = Regex("""\[download]\s+(\d+(?:\.\d+)?)%""")
    private val ansi = Regex("""\u001B\[[0-9;]*[A-Za-z]""")

    fun parse(line: String): DownloadProgress? {
        val text = line.replace(ansi, "").trim()
        template.find(text)?.let { m ->
            val down = m.groupValues[1].toLongOrNull()
            val total = m.groupValues[2].toLongOrNull()
            val estimate = m.groupValues[3].toLongOrNull()
            val fragIndex = m.groupValues[4].toLongOrNull()
            val fragCount = m.groupValues[5].toLongOrNull()
            val pct = m.groupValues[6].removeSuffix("%").toFloatOrNull()
            val percent = when {
                down != null && down > 0 && total != null && total > 0 ->
                    (down * 100 / total).toInt()
                down != null && down > 0 && estimate != null && estimate > 0 ->
                    (down * 100 / estimate).toInt()
                fragIndex != null && fragCount != null && fragCount > 0 && fragIndex >= 0 ->
                    (fragIndex * 100 / fragCount).toInt()
                pct != null && !pct.isNaN() -> pct.toInt()
                else -> -1
            }
            val speed = clean(m.groupValues[7])
            val eta = clean(m.groupValues[8])
            val detail = listOfNotNull(speed, eta.takeIf { it.isNotBlank() }?.let { "ETA $it" })
                .joinToString(" · ")
            return DownloadProgress(
                if (percent < 0) -1 else percent.coerceIn(0, 100),
                detail
            )
        }
        legacy.find(text)?.let { m ->
            val percent = m.groupValues[1].toFloatOrNull() ?: return null
            return DownloadProgress(percent.toInt().coerceIn(0, 100), text.take(60))
        }
        return null
    }

    private fun clean(value: String): String =
        value.takeIf { it != "NA" && it.isNotBlank() && it != "None" } ?: ""
}
