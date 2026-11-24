package app.ytcutter

import android.app.Application
import android.content.Context
import app.ytcutter.core.data.ComponentRepository
import app.ytcutter.core.data.RemoteConfigRepository
import app.ytcutter.core.data.work.UpdateCheckWorker
import app.ytcutter.core.datastore.UpdatePrefs

object AppGraph {

    fun init(application: Application) {
        UpdateCheckWorker.schedule(application)
    }

    fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "0.0.0"

    fun updatePrefs(context: Context): UpdatePrefs = UpdatePrefs(context)

    @Volatile
    private var remote: RemoteConfigRepository? = null

    fun remoteConfig(context: Context): RemoteConfigRepository =
        remote ?: synchronized(this) {
            remote ?: RemoteConfigRepository(context.applicationContext, OWNER, REPO)
                .also { remote = it }
        }

    fun componentRepository(context: Context): ComponentRepository =
        ComponentRepository(context.applicationContext, appVersion(context))

    private const val OWNER = "nurfilardi"
    private const val REPO = "ytcutter"
}