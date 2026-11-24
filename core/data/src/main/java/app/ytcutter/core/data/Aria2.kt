package app.ytcutter.core.data

import android.content.Context
import com.yausername.aria2c.Aria2c
import java.io.File

/**
 * aria2c multi-koneksi (cara yang dipakai YTDLnis). Biner datang dari artefak
 * youtubedl-android dan dieksekusi dari nativeLibraryDir, jadi tidak perlu
 * unduh biner saat runtime. Bila tidak tersedia, unduhan tetap pakai yt-dlp native.
 */
object Aria2 {

    @Volatile
    private var binary: String? = null

    @Volatile
    private var cert: String? = null

    fun init(context: Context) {
        runCatching { Aria2c.getInstance().init(context.applicationContext) }
        val bin = File(context.applicationInfo.nativeLibraryDir, "libaria2c.so")
        if (bin.isFile && bin.canExecute()) binary = bin.absolutePath
        val pem = File(
            File(File(context.noBackupFilesDir, "youtubedl-android/packages/python"), "usr/etc/tls"),
            "cert.pem"
        )
        if (pem.isFile) cert = pem.absolutePath
    }

    fun available(): Boolean = binary != null

    fun args(fragments: Int): List<Pair<String, String?>> {
        val path = binary ?: return emptyList()
        val connections = fragments.coerceIn(1, 16)
        val options = buildString {
            append("-x $connections -s $connections -j $connections -k 10M")
            append(" --file-allocation=none --summary-interval=0")
            append(" --console-log-level=warn --connect-timeout=15 --timeout=20")
            cert?.let { append(" --ca-certificate=$it") }
        }
        return listOf(
            "--downloader" to path,
            "--external-downloader-args" to "aria2c:$options"
        )
    }
}
