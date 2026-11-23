package app.ytcutter.data

import app.ytcutter.data.cookies.CookiePrefs
import android.content.Context
import java.io.File

object CookieGate {

    fun path(file: File?, enabled: Boolean): String? =
        file?.takeIf { enabled && it.exists() && it.length() > 0 }?.absolutePath

    fun active(context: Context): String? =
        path(CookieStore.file(context), CookiePrefs.useCookies(context))
}
