package app.ytcutter.core.datastore

import android.content.Context

class UserPrefs(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun useCookies(): Boolean = migrate(oldValue(appContext), stored())

    fun setUseCookies(on: Boolean) {
        prefs.edit().putBoolean(KEY_USE_COOKIES, on).apply()
    }

    private fun stored(): Boolean? =
        if (prefs.contains(KEY_USE_COOKIES)) prefs.getBoolean(KEY_USE_COOKIES, false) else null

    companion object {
        private const val PREFS = "users"
        private const val KEY_USE_COOKIES = "use_cookies"
        private const val LEGACY_PREFS = "settings"

        fun migrate(old: Boolean?, stored: Boolean?): Boolean = stored ?: old ?: false

        private fun oldValue(context: Context): Boolean? {
            val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            return if (legacy.contains(KEY_USE_COOKIES)) {
                legacy.getBoolean(KEY_USE_COOKIES, false)
            } else {
                null
            }
        }
    }
}