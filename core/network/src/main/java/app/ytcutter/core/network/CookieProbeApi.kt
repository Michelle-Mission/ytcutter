package app.ytcutter.core.network

import app.ytcutter.core.model.CookieStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object CookieProbeApi {

    private const val PROBE_URL = "https://www.youtube.com/feed/subscriptions"

    fun classify(finalUrl: String?, code: Int): CookieStatus {
        if (finalUrl == null) return CookieStatus.UNKNOWN
        if (code !in 200..399) return CookieStatus.UNKNOWN
        val host = runCatching { java.net.URI(finalUrl).host }.getOrNull()
            ?: return CookieStatus.UNKNOWN
        return when {
            host == "consent.youtube.com" -> CookieStatus.UNKNOWN
            host == "accounts.google.com" || host.endsWith(".accounts.google.com") ->
                CookieStatus.NOT_LOGGED_IN
            host == "youtube.com" || host.endsWith(".youtube.com") -> CookieStatus.LOGGED_IN
            else -> CookieStatus.UNKNOWN
        }
    }

    suspend fun probe(cookieHeader: String): CookieStatus = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(PROBE_URL)
            .header("Cookie", cookieHeader)
            .build()
        runCatching {
            HttpClient.client.newCall(request).execute().use { response ->
                classify(response.request.url.toString(), response.code)
            }
        }.getOrDefault(CookieStatus.UNKNOWN)
    }
}