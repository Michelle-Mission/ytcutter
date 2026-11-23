package app.ytcutter.core.data

import app.ytcutter.core.common.Version
import app.ytcutter.core.datastore.UpdatePrefs
import app.ytcutter.core.model.ReleaseInfo
import app.ytcutter.core.network.GitHubReleasesApi

class AppUpdateRepository(
    private val prefs: UpdatePrefs,
    private val api: GitHubReleasesApi
) {

    suspend fun check(currentVersion: String): ReleaseInfo? {
        prefs.lastCheckMs = System.currentTimeMillis()
        val latest = api.latest() ?: return null
        prefs.latestVersion = latest.version
        val current = Version.parse(currentVersion) ?: return null
        val newer = Version.parse(latest.version) ?: return null
        return if (newer > current) latest else null
    }

    fun isIgnored(release: ReleaseInfo): Boolean = prefs.ignoredVersion == release.version

    fun ignore(release: ReleaseInfo) {
        prefs.ignoredVersion = release.version
    }
}
