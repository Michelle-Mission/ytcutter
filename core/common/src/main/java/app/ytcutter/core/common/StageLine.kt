package app.ytcutter.core.common

/**
 * Menerjemahkan baris mentah yt-dlp menjadi tahap kerja yang terlihat pengguna,
 * supaya tidak hanya "Berjalan" lalu loncat 0% → 100%.
 */
object StageLine {

    private val rules = listOf(
        "extracting url" to "Mengambil tautan video",
        "[youtube]" to "Mengambil info video",
        "[info] downloading" to "Menyiapkan format",
        "cutting" to "Memotong video",
        "[merger]" to "Menggabungkan video dan audio",
        "[extractaudio]" to "Mengekstrak audio",
        "[videoremuxer]" to "Mengubah format video",
        "[fixupm3u8]" to "Memperbaiki format video",
        "[videoconvertor]" to "Mengonversi video",
        "[muxer]" to "Menggabungkan berkas",
        "[metadata]" to "Menulis metadata",
        "[embedthumbnail]" to "Menempelkan sampul",
        "[modifychapters]" to "Memangkas bagian video",
        "deleting original file" to "Merapikan berkas",
        "[movefiles]" to "Menyimpan berkas",
        "destination:" to "Mengunduh berkas",
        "has already been downloaded" to "Berkas sudah ada, memakai ulang"
    )

    fun parse(line: String): String? {
        val lower = line.trim().lowercase()
        if (lower.isEmpty()) return null
        return rules.firstOrNull { (needle, _) -> lower.contains(needle) }?.second
    }
}
