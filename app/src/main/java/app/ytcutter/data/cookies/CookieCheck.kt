package app.ytcutter.data.cookies

import android.content.Context
import app.ytcutter.data.NetscapeCookies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class CookieStatus { LOGGED_IN, NOT_LOGGED_IN, UNKNOWN }

object CookieCheck {

    private const val PROBE_URL = "https://www.youtube.com/feed/subscriptions"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

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

    suspend fun probe(context: Context): CookieStatus = withContext(Dispatchers.IO) {
        val header = NetscapeCookies.header(CookiesRepo.get(context).buildEnabledText().lines())
            ?: return@withContext CookieStatus.UNKNOWN
        val request = Request.Builder()
            .url(PROBE_URL)
            .header("Cookie", header)
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                classify(response.request.url.toString(), response.code)
            }
        }.getOrDefault(CookieStatus.UNKNOWN)
    }

}