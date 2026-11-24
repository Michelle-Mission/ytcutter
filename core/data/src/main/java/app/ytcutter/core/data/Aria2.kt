package app.ytcutter.core.data

import app.ytcutter.core.data.engine.Engine

/** aria2c multi-koneksi; biner dari APK helper (atau bawaan APK) via [Engine]. */
object Aria2 {

    fun available(context: android.content.Context): Boolean =
        runCatching { Engine.prepare(context).aria2Bin != null }.getOrDefault(false)

    fun args(context: android.content.Context, fragments: Int): List<Pair<String, String?>> {
        val layout = runCatching { Engine.prepare(context) }.getOrNull() ?: return emptyList()
        val bin = layout.aria2Bin ?: return emptyList()
        val connections = fragments.coerceIn(1, 16)
        val options = buildString {
            append("-x $connections -s $connections -j $connections -k 10M")
            append(" --file-allocation=none --summary-interval=0")
            append(" --console-log-level=warn --connect-timeout=15 --timeout=20")
            layout.cert?.let { append(" --ca-certificate=${it.absolutePath}") }
        }
        return listOf(
            "--downloader" to bin.absolutePath,
            "--external-downloader-args" to "aria2c:$options"
        )
    }
}
