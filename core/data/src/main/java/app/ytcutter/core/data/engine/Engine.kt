package app.ytcutter.core.data.engine

import android.content.Context
import java.io.File
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Menjalankan yt-dlp dengan komponen (python, ffmpeg, aria2c) dari APK helper
 * terpisah seperti YTDLnis, dengan cadangan biner bawaan APK bila helper absen.
 */
object Engine {

    const val HELPER_PACKAGE = "app.ytcutter.components"
    private const val BASE = "youtubedl-android"

    data class Layout(
        val pythonBin: File,
        val pythonHome: File,
        val ffmpegBin: File,
        val aria2Bin: File?,
        val cert: File?,
        val script: File,
        val binDir: File,
        val helperVersion: String?
    )

    private val processes = Collections.synchronizedMap(HashMap<String, Process>())

    @Volatile
    private var layout: Layout? = null

    fun helperVersion(context: Context): String? = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(HELPER_PACKAGE, 0).versionName
    }.getOrNull()

    fun prepare(context: Context): Layout {
        layout?.let { return it }
        val app = context.applicationContext
        val bundledDir = File(app.applicationInfo.nativeLibraryDir)
        val helperDir = helperNativeDir(app) ?: bundledDir
        val root = File(app.noBackupFilesDir, "engine").apply { mkdirs() }
        val pythonHome = unpack(File(helperDir, "libpython.zip.so"), File(root, "python"))
        val ffmpegHome = unpack(File(helperDir, "libffmpeg.zip.so"), File(root, "ffmpeg"))
        val aria2Home = unpack(File(helperDir, "libaria2c.zip.so"), File(root, "aria2c"))
        val cert = File(pythonHome, "usr/etc/tls/cert.pem").takeIf { it.isFile }
        val script = File(File(app.noBackupFilesDir, "$BASE/yt-dlp"), "yt-dlp")
        val result = Layout(
            pythonBin = File(helperDir, "libpython.so"),
            pythonHome = pythonHome,
            ffmpegBin = File(helperDir, "libffmpeg.so"),
            aria2Bin = File(helperDir, "libaria2c.so").takeIf { it.isFile },
            cert = cert,
            script = script,
            binDir = helperDir,
            helperVersion = helperVersion(app)
        )
        layout = result
        return result
    }

    fun invalidate() {
        layout = null
    }

    private fun helperNativeDir(context: Context): File? = runCatching {
        val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                HELPER_PACKAGE,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(HELPER_PACKAGE, 0)
        }
        File(info.applicationInfo!!.nativeLibraryDir)
    }.getOrNull()

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
