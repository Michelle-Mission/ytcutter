package app.ytcutter.core.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.ytcutter.core.common.Version
import app.ytcutter.core.data.AppUpdateRepository
import app.ytcutter.core.datastore.UpdatePrefs
import app.ytcutter.core.network.GitHubReleasesApi
import app.ytcutter.core.notifications.UpdateNotifications
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = UpdatePrefs(context)
        val repo = AppUpdateRepository(prefs, GitHubReleasesApi(OWNER, REPO))
        val current = runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "0.0.0"
        }.getOrDefault("0.0.0")

        val release = runCatching { repo.check(current) }.getOrNull() ?: return Result.success()
        val currentVersion = Version.parse(current)
        val isNewer = Version.parse(release.version)?.let {
            currentVersion == null || it > currentVersion
        } ?: false
        if (isNewer && !repo.isIgnored(release)) {
            UpdateNotifications.notifyAvailable(context, release.version, release.url)
        }
        return Result.success()
    }

    companion object {
        const val OWNER = "nurfilardi"
        const val REPO = "ytcutter"
        private const val WORK = "update-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
