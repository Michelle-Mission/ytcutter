package app.ytcutter.core.datastore

import android.content.Context

class UpdatePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var lastCheckMs: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    var latestVersion: String?
        get() = prefs.getString(KEY_LATEST, null)
        set(value) = prefs.edit().putString(KEY_LATEST, value).apply()

    var ignoredVersion: String?
        get() = prefs.getString(KEY_IGNORED, null)
        set(value) = prefs.edit().putString(KEY_IGNORED, value).apply()

    companion object {
        private const val PREFS = "updates"
        private const val KEY_LAST_CHECK = "last_check_ms"
        private const val KEY_LATEST = "latest_version"
        private const val KEY_IGNORED = "ignored_version"
    }
}