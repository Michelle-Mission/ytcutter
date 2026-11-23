package app.ytcutter.core.data

import android.content.Context
import android.content.Intent
import java.io.File

object CrashLog {

    private const val CRASH = "last-crash.txt"
    private const val BOOTING = "booting.marker"

    private fun crashFile(context: Context) = File(context.filesDir, CRASH)
    private fun markerFile(context: Context) = File(context.filesDir, BOOTING)

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val booting = markerFile(app).exists()
                crashFile(app).writeText(
                    buildString {
                        appendLine("waktu: ${System.currentTimeMillis()}")
                        appendLine("fase: ${if (booting) "boot/init mesin" else "runtime"}")
                        appendLine(throwable.stackTraceToString())
                    }
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun markBooting(context: Context) {
        runCatching { markerFile(context).writeText("${System.currentTimeMillis()}") }
    }

    fun markBooted(context: Context) {
        runCatching { markerFile(context).delete() }
    }

    fun crashedWhileBooting(context: Context): Boolean =
        markerFile(context).exists() && crashFile(context).exists()

    fun last(context: Context): String? = runCatching {
        crashFile(context).takeIf { it.exists() }?.readText()
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { crashFile(context).delete() }
    }

    fun share(context: Context, text: String) {
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text.take(24000))
                    },
                    "Bagikan log"
                )
            )
        }
    }
}