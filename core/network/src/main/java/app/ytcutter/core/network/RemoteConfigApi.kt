package app.ytcutter.core.network

import app.ytcutter.core.model.Announcement
import app.ytcutter.core.model.AppConfig
import app.ytcutter.core.model.ChangeEntry
import app.ytcutter.core.model.FeatureFlags
import app.ytcutter.core.model.Maintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class RemoteConfigApi(owner: String, repo: String) {

    private val base = "https://$owner.github.io/$repo"

    suspend fun rawConfig(): String? = fetch("config.json")
    suspend fun rawChangelog(): String? = fetch("changelog.json")

    fun parseConfig(body: String): AppConfig? = runCatching {
        val json = JSONObject(body)
        val maintenance = json.optJSONObject("maintenance")
        val features = json.optJSONObject("features")
        val announcement = json.optJSONObject("announcement")?.takeIf {
            it.optString("id").isNotBlank() && it.optString("message").isNotBlank()
        }?.let {
            Announcement(
                id = it.getString("id"),
                title = it.optString("title"),
                message = it.getString("message"),
                url = it.optString("url").takeIf(String::isNotBlank)
            )
        }
        AppConfig(
            schemaVersion = json.optInt("schemaVersion", 1),
            maintenance = Maintenance(
                enabled = maintenance?.optBoolean("enabled") == true,
                message = maintenance?.optString("message").orEmpty()
            ),
            announcement = announcement,
            features = FeatureFlags(
                downloader = features?.optBoolean("downloader", true) ?: true,
                player = features?.optBoolean("player", true) ?: true
            ),
            minVersionCode = json.optLong("minVersionCode", 0)
        )
    }.getOrNull()

    fun parseChangelog(body: String): List<ChangeEntry>? = runCatching {
        val array = JSONObject(body).getJSONArray("entries")
        (0 until array.length()).map { i ->
            val entry = array.getJSONObject(i)
            val items = entry.getJSONArray("items")
            ChangeEntry(
                label = entry.getString("label"),
                items = (0 until items.length()).map(items::getString)
            )
        }
    }.getOrNull()

    private suspend fun fetch(name: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/$name")
                .header("Accept", "application/json")
                .build()
            HttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.getOrNull()
    }
}
