package app.ytcutter.data.cookies

import android.content.Context

object CookiePrefs {

    private const val PREFS = "settings"
    private const val KEY_USE_COOKIES = "use_cookies"

    fun useCookies(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_COOKIES, false)

    fun setUseCookies(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_COOKIES, on)
            .apply()
    }
}