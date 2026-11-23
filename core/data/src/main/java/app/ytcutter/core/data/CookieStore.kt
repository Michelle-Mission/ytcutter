package app.ytcutter.core.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream

object CookieStore {

    private const val FILE_NAME = "cookies.txt"

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun path(context: Context): String? =
        file(context).takeIf { it.exists() && it.length() > 0 }?.absolutePath

    fun importFrom(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            save(context, input)
        } ?: false
    }.getOrDefault(false)

    fun save(context: Context, input: InputStream): Boolean = runCatching {
        val target = file(context)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.outputStream().use { out -> input.copyTo(out) }
        val looksLikeCookies = temp.length() > 0
        if (looksLikeCookies) {
            target.delete()
            temp.renameTo(target)
            true
        } else {
            temp.delete()
            false
        }
    }.getOrDefault(false)

    fun saveText(context: Context, text: String): Boolean = runCatching {
        val target = file(context)
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        temp.writeText(text)
        val ok = temp.length() > 0
        if (ok) {
            target.delete()
            temp.renameTo(target)
        } else {
            temp.delete()
        }
        ok
    }.getOrDefault(false)

    fun load(context: Context): String? = runCatching {
        file(context).takeIf { it.exists() }?.readText()
    }.getOrNull()

    fun clear(context: Context) {
        file(context).delete()
    }
}