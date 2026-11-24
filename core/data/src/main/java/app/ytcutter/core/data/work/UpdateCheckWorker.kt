package app.ytcutter.core.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.ytcutter.core.data.update.AppUpdater
import app.ytcutter.core.datastore.UpdatePrefs
import app.ytcutter.core.notifications.UpdateNotifications
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = UpdatePrefs(context)
        val updater = AppUpdater(context)
        val update = runCatching { updater.check() }.getOrNull() ?: return Result.success()
        prefs.latestVersion = update.versionName
        prefs.lastCheckMs = System.currentTimeMillis()
        if (prefs.ignoredVersion != update.versionCode.toString()) {
            UpdateNotifications.notifyAvailable(
                context,
                update.versionName,
                "https://github.com/${AppUpdater.OWNER}/${AppUpdater.REPO}/releases/latest"
            )
        }
        return Result.success()
    }

    companion object {
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
