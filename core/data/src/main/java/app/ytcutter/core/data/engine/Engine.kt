package app.ytcutter.core.data.engine

import android.content.Context
import java.io.File
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Menjalankan yt-dlp dengan komponen (python, ffmpeg, aria2c) yang ikut di APK,
 * memakai interpreter dari nativeLibraryDir agar lolos batasan exec Android 10+.
 */
object Engine {

    private const val BASE = "youtubedl-android"

    data class Layout(
        val pythonBin: File,
        val pythonHome: File,
        val ffmpegBin: File,
        val aria2Bin: File?,
        val cert: File?,
        val script: File,
        val binDir: File
    )

    private val processes = Collections.synchronizedMap(HashMap<String, Process>())

    @Volatile
    private var layout: Layout? = null

    fun prepare(context: Context): Layout {
        layout?.let { return it }
        val app = context.applicationContext
        val bundledDir = File(app.applicationInfo.nativeLibraryDir)
        val root = File(app.noBackupFilesDir, "engine").apply { mkdirs() }
        val pythonHome = unpack(File(bundledDir, "libpython.zip.so"), File(root, "python"))
        val ffmpegHome = unpack(File(bundledDir, "libffmpeg.zip.so"), File(root, "ffmpeg"))
        val aria2Home = unpack(File(bundledDir, "libaria2c.zip.so"), File(root, "aria2c"))
        val cert = File(pythonHome, "usr/etc/tls/cert.pem").takeIf { it.isFile }
        val script = File(File(app.noBackupFilesDir, "$BASE/yt-dlp"), "yt-dlp")
        val result = Layout(
            pythonBin = File(bundledDir, "libpython.so"),
            pythonHome = pythonHome,
            ffmpegBin = File(bundledDir, "libffmpeg.so"),
            aria2Bin = File(bundledDir, "libaria2c.so").takeIf { it.isFile },
            cert = cert,
            script = script,
            binDir = bundledDir
        )
        layout = result
        return result
    }

    fun invalidate() {
        layout = null
    }


    private fun unpack(zip: File, dest: File): File {
        if (!zip.isFile) return dest
        val marker = File(dest, ".version")
        val stamp = "${zip.length()}"
        if (marker.isFile && marker.readText() == stamp) return dest
        dest.deleteRecursively()
        dest.mkdirs()
        runCatching {
            ZipInputStream(zip.inputStream().buffered()).use { input ->
                var entry = input.nextEntry
                while (entry != null) {
                    val target = File(dest, entry.name)
                    if (!target.canonicalPath.startsWith(dest.canonicalPath + File.separator)) {
                        entry = input.nextEntry
                        continue
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { out -> input.copyTo(out) }
                    }
                    entry = input.nextEntry
                }
            }
            marker.writeText(stamp)
        }
        return dest
    }

    fun execute(
        context: Context,
        args: List<String>,
        processId: String,
        onLine: (String) -> Unit
    ): Int {
        val app = context.applicationContext
        val l = prepare(app)
        require(l.pythonBin.isFile) { "Komponen python tidak ditemukan" }
        require(l.script.isFile) { "yt-dlp belum siap" }
        val command = ArrayList<String>(args.size + 2)
        command += l.pythonBin.absolutePath
        command += l.script.absolutePath
        command += args
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .apply {
                val env = environment()
                env["LD_LIBRARY_PATH"] = listOf(
                    File(l.pythonHome, "usr/lib"),
                    File(l.pythonHome, "lib"),
                    File(File(app.noBackupFilesDir, "engine/ffmpeg"), "usr/lib"),
                    File(File(app.noBackupFilesDir, "engine/aria2c"), "usr/lib")
                ).joinToString(":") { it.absolutePath }
                l.cert?.let { env["SSL_CERT_FILE"] = it.absolutePath }
                env["PYTHONHOME"] = File(l.pythonHome, "usr").absolutePath
                env["HOME"] = File(l.pythonHome, "usr").absolutePath
                env["TMPDIR"] = app.cacheDir.absolutePath
                env["PATH"] = (System.getenv("PATH") ?: "/system/bin") +
                    ":" + l.binDir.absolutePath
            }
            .start()
        processes[processId] = process
        val stderr = StringBuilder()
        val errThread = Thread {
            runCatching {
                process.errorStream.bufferedReader().forEachLine { line ->
                    if (stderr.length < 64_000) stderr.appendLine(line)
                }
            }
        }.apply { isDaemon = true; start() }
        try {
            process.inputStream.bufferedReader().forEachLine { line ->
                if (line.isNotBlank()) onLine(line)
            }
            if (!process.waitFor(30, TimeUnit.MINUTES)) process.destroyForcibly()
            if (process.isAlive) process.waitFor(5, TimeUnit.SECONDS)
            errThread.join(2_000)
            return runCatching { process.exitValue() }.getOrDefault(-1)
        } finally {
            processes.remove(processId)
        }
    }

    fun cancel(processId: String?): Boolean {
        if (processId == null) return false
        return processes.remove(processId)?.let { runCatching { it.destroy(); true }.getOrDefault(false) }
            ?: false
    }
}
