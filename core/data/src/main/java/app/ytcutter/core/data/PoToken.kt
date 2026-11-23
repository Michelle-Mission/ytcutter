package app.ytcutter.core.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import app.ytcutter.core.common.UrlValidator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * BotGuard poToken generator (alur yang sama dipakai YTDLnis/NewPipe): jalankan VM YouTube
 * di WebView, tukar hasilnya jadi integrity token lewat JNN, lalu cetak poToken untuk
 * streaming + video. Hasilnya diteruskan ke yt-dlp lewat --extractor-args.
 */
object PoToken {

    private const val API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
    private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
    private const val CLIENT_VERSION = "2.20250101.00.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.3"

    private val mutex = Mutex()

    @Volatile
    private var session: Session? = null

    private class Session(
        val web: WebView,
        val visitorData: String,
        val streaming: String,
        val player: MutableMap<String, String> = HashMap(),
        val expiresAt: Long
    )

    data class Tokens(val visitorData: String, val streaming: String, val player: String)

    fun buildExtractorArgs(tokens: Tokens): String =
        "youtube:player_client=default,-tv_downgraded;" +
            "po_token=web.gvs+${tokens.streaming},web.player+${tokens.player};" +
            "visitor_data=${tokens.visitorData}"

    suspend fun extractorArgs(context: Context, url: String): String? =
        tokens(context, url)?.let(::buildExtractorArgs)

    suspend fun tokens(context: Context, url: String): Tokens? {
        val videoId = UrlValidator.videoId(url) ?: return null
        return runCatching { withTimeout(25_000) { withContext(Dispatchers.IO) { sessionTokens(context, videoId) } } }
            .getOrNull()
    }

    private suspend fun sessionTokens(context: Context, videoId: String): Tokens {
        val app = context.applicationContext
        return mutex.withLock {
            val now = System.currentTimeMillis()
            var s = session?.takeIf { it.expiresAt > now }
            if (s == null) {
                s = createSession(app)
                session = s
            }
            val player = s.player[videoId] ?: mint(s.web, videoId).also { s.player[videoId] = it }
            Tokens(s.visitorData, s.streaming, player)
        }
    }

    private suspend fun createSession(context: Context): Session {
        val visitorData = visitorData()
        val challenge = challenge()
        val web = createWeb(context)
        val response = awaitSnapshot(web, challenge)
        val (integrity, ttl) = integrityToken(response)
        setIntegrity(web, integrity)
        // sisakan 10 menit margin
        val expiresAt = System.currentTimeMillis() + ((ttl - 600).coerceAtLeast(300)) * 1000
        val streaming = mint(web, visitorData)
        return Session(web, visitorData, streaming, expiresAt = expiresAt)
    }

    private suspend fun visitorData(): String {
        val body = """{"context":{"client":{"clientName":"WEB","clientVersion":"$CLIENT_VERSION"}}}"""
        val json = JSONObject(post("https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false", body, mapOf(), "application/json"))
        return json.getJSONObject("responseContext").getString("visitorData")
    }

    private suspend fun challenge(): String {
        val raw = post(
            "https://www.youtube.com/api/jnn/v1/Create",
            JSONArray().put(REQUEST_KEY).toString(),
            mapOf(
                "x-goog-api-key" to API_KEY,
                "x-user-agent" to "grpc-web-javascript/0.1"
            ),
            "application/json+protobuf"
        )
        val scrambled = JSONArray(raw)
        val data = when {
            scrambled.length() > 1 && !scrambled.isNull(1) && scrambled.get(1) is String ->
                JSONArray(descramble(scrambled.getString(1)))
            scrambled.length() > 1 && !scrambled.isNull(1) -> scrambled.getJSONArray(1)
            else -> error("challenge kosong")
        }
        val interpreter = data.optJSONArray(1)?.let { arr ->
            (0 until arr.length()).map { arr.opt(it) }.firstOrNull { it is String } as? String
        }
        return JSONObject().apply {
            put("messageId", data.optString(0))
            put("interpreterJavascript", JSONObject().put("privateDoNotAccessOrElseSafeScriptWrappedValue", interpreter))
            put("interpreterHash", data.optString(3))
            put("program", data.optString(4))
            put("globalName", data.optString(5))
        }.toString()
    }

    private suspend fun integrityToken(botguardResponse: String): Pair<String, Long> {
        val raw = post(
            "https://www.youtube.com/api/jnn/v1/GenerateIT",
            JSONArray().put(REQUEST_KEY).put(botguardResponse).toString(),
            mapOf(
                "x-goog-api-key" to API_KEY,
                "x-user-agent" to "grpc-web-javascript/0.1"
            ),
            "application/json+protobuf"
        )
        val arr = JSONArray(raw)
        return arr.getString(0) to arr.optLong(1, 21_600L)
    }

    private fun post(url: String, body: String, headers: Map<String, String>, contentType: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", contentType)
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun descramble(value: String): String {
        val normalized = value.replace('-', '+').replace('_', '/').replace('.', '=')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val bytes = Base64.getDecoder().decode(padded)
        return String(bytes.map { (it + 97).toByte() }.toByteArray())
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun createWeb(context: Context): WebView = withContext(Dispatchers.Main) {
        val web = WebView(context)
        web.settings.javaScriptEnabled = true
        web.settings.blockNetworkLoads = true
        web.settings.userAgentString = USER_AGENT
        web.addJavascriptInterface(Bridge(), "YTC")
        val loaded = CompletableDeferred<Unit>()
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                loaded.complete(Unit)
            }
        }
        val html = context.assets.open("po_token.html").bufferedReader().use { it.readText() }
        web.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
        loaded.await()
        web
    }

    private suspend fun awaitSnapshot(web: WebView, challengeJson: String): String {
        Bridge.snapshotWaiter = null
        withContext(Dispatchers.Main) {
            web.evaluateJavascript("window.__run($challengeJson)", null)
        }
        return withTimeout(20_000) {
            suspendCancellableCoroutine { cont ->
                Bridge.snapshotWaiter = cont
                cont.invokeOnCancellation { Bridge.snapshotWaiter = null }
            }
        }
    }

    private suspend fun setIntegrity(web: WebView, integrity: String) {
        val normalized = integrity.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val bytes = Base64.getDecoder().decode(padded).joinToString(",") { (it.toInt() and 0xFF).toString() }
        withContext(Dispatchers.Main) {
            web.evaluateJavascript("window.__integrity = new Uint8Array([$bytes])", null)
        }
    }

    private suspend fun mint(web: WebView, identifier: String): String {
        return withTimeout(15_000) {
            suspendCancellableCoroutine { cont ->
                Bridge.tokenWaiters[identifier] = cont
                cont.invokeOnCancellation { Bridge.tokenWaiters.remove(identifier) }
                Handler(Looper.getMainLooper()).post {
                    web.evaluateJavascript("window.__mint(${JSONObject.quote(identifier)})", null)
                }
            }
        }
    }

    private class Bridge {
        @JavascriptInterface
        fun onSnapshot(response: String?) {
            snapshotWaiter?.let {
                snapshotWaiter = null
                it.resume(response.orEmpty())
            }
        }

        @JavascriptInterface
        fun onToken(identifier: String?, token: String?) {
            if (identifier == null || token == null) return
            tokenWaiters.remove(identifier)?.resume(token)
        }

        @JavascriptInterface
        fun onError(message: String?) {
            snapshotWaiter?.let {
                snapshotWaiter = null
                it.resume("")
            }
        }

        companion object {
            @Volatile
            var snapshotWaiter: Continuation<String>? = null

            val tokenWaiters = java.util.concurrent.ConcurrentHashMap<String, Continuation<String>>()
        }
    }
}