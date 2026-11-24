package app.ytcutter.core.data

import android.content.Context
import app.ytcutter.core.model.AppConfig
import app.ytcutter.core.model.ChangeEntry
import app.ytcutter.core.network.RemoteConfigApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Konfigurasi publik dari GitHub Pages. Online-first, cache disk sebagai
 * cadangan; kegagalan tidak boleh menghentikan aplikasi.
 */
class RemoteConfigRepository(context: Context, owner: String, repo: String) {

    private val api = RemoteConfigApi(owner, repo)
    private val dir = File(context.applicationContext.cacheDir, "remote").apply { mkdirs() }

    @Volatile
    var online: Boolean = false
        private set

    @Volatile
    var lastSyncMs: Long = 0L
        private set

    @Volatile
    var lastError: String? = null
        private set

    suspend fun config(): AppConfig = withContext(Dispatchers.IO) {
        val file = File(dir, "config.json")
        val raw = withTimeoutOrNull(8_000) { api.rawConfig() }
        val fresh = raw?.let(api::parseConfig)
        if (raw != null && fresh != null) {
            runCatching { file.writeText(raw) }
            succeeded()
            return@withContext fresh
        }
        failed("config tidak terjangkau")
        runCatching { api.parseConfig(file.readText()) }.getOrNull() ?: AppConfig.DEFAULT
    }

    suspend fun changelog(): List<ChangeEntry>? = withContext(Dispatchers.IO) {
        val file = File(dir, "changelog.json")
        val raw = withTimeoutOrNull(8_000) { api.rawChangelog() }
        val fresh = raw?.let(api::parseChangelog)
        if (raw != null && fresh != null) {
            runCatching { file.writeText(raw) }
            succeeded()
            return@withContext fresh
        }
        failed("changelog tidak terjangkau")
        runCatching { api.parseChangelog(file.readText()) }.getOrNull()
    }

    private fun succeeded() {
        online = true
        lastError = null
        lastSyncMs = System.currentTimeMillis()
    }

    private fun failed(reason: String) {
        online = false
        lastError = reason
    }

    fun statusText(): String = when {
        online -> "Online"
        lastError != null -> "Offline"
        else -> "Belum dicek"
    }
}
