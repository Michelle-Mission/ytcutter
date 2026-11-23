package app.ytcutter.core.network

import app.ytcutter.core.common.Version
import app.ytcutter.core.model.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class GitHubReleasesApi(
    private val owner: String,
    private val repo: String
) {

    suspend fun latest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        runCatching {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                val tag = json.optString("tag_name")
                val version = Version.parse(tag) ?: return@use null
                ReleaseInfo(
                    version = version.toString(),
                    tag = tag,
                    url = json.optString("html_url"),
                    notes = json.optString("body")
                )
            }
        }.getOrNull()
    }
}