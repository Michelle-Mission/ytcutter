package app.ytcutter.core.network

import app.ytcutter.core.model.AppUpdate
import app.ytcutter.core.model.UpdateAsset
import app.ytcutter.core.model.UpdatePatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class UpdateManifestApi(
    private val owner: String,
    private val repo: String
) {

    suspend fun latest(): AppUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://github.com/$owner/$repo/releases/latest/download/update.json")
            .header("Accept", "application/json")
            .build()
        runCatching {
            HttpClient.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                parse(response.body?.string() ?: return@use null)
            }
        }.getOrNull()
    }

    internal fun parse(body: String): AppUpdate? = runCatching {
        val json = JSONObject(body)
        val apk = json.getJSONObject("apk").toAsset()
        val patch = json.optJSONObject("patch")?.let {
            UpdatePatch(it.toAsset(), it.getLong("fromVersionCode"))
        }
        AppUpdate(
            versionName = json.getString("versionName"),
            versionCode = json.getLong("versionCode"),
            notes = json.optString("notes"),
            apk = apk,
            patch = patch
        )
    }.getOrNull()

    private fun JSONObject.toAsset() = UpdateAsset(
        name = getString("name"),
        url = getString("url"),
        sha256 = getString("sha256"),
        size = getLong("size")
    )
}