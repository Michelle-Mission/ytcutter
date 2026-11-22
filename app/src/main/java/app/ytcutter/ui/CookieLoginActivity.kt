package app.ytcutter.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.ytcutter.core.CookieExport
import app.ytcutter.core.CookieRecord
import app.ytcutter.data.CookieStore
import app.ytcutter.ui.theme.YtCutterTheme

class CookieLoginActivity : ComponentActivity() {

    private val captured = LinkedHashMap<String, CookieRecord>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YtCutterTheme {
                Surface(Modifier.fillMaxSize()) {
                    var count by remember { mutableIntStateOf(0) }
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                            }
                            Text(
                                "Login YouTube",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    if (save()) {
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                }
                            ) { Text("Selesai") }
                        }
                        Text(
                            "Login, lalu tap Selesai. Cookies hanya disimpan di perangkat ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        AndroidView(
                            modifier = Modifier.weight(1f),
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.userAgentString = spoofedUserAgent()
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            request: WebResourceRequest
                                        ): WebResourceResponse? {
                                            val host = request.url.host ?: return null
                                            val header = request.requestHeaders?.get("Cookie") ?: return null
                                            val domain = when {
                                                host.endsWith("youtube.com") -> ".youtube.com"
                                                host.endsWith("google.com") -> ".google.com"
                                                else -> return null
                                            }
                                            CookieExport.fromCookieHeader(header, domain, expiry())
                                                .forEach { captured["${it.domain}|${it.name}"] = it }
                                            count = captured.size
                                            return null
                                        }
                                    }
                                    loadUrl(
                                        "https://accounts.google.com/ServiceLogin" +
                                            "?continue=https%3A%2F%2Fm.youtube.com%2F"
                                    )
                                }
                            }
                        )
                        Text(
                            if (count > 0) "$count cookies tertangkap" else "Belum ada cookies tertangkap",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    private fun save(): Boolean {
        val manager = CookieManager.getInstance()
        manager.flush()
        listOf(".youtube.com" to "https://www.youtube.com", ".google.com" to "https://accounts.google.com")
            .forEach { (domain, url) ->
                manager.getCookie(url)?.let { header ->
                    CookieExport.fromCookieHeader(header, domain, expiry())
                        .forEach { captured["${it.domain}|${it.name}"] = it }
                }
            }
        if (captured.isEmpty()) return false
        return CookieStore.saveText(
            this,
            CookieExport.toNetscape(captured.values)
        )
    }

    private fun expiry(): Long = System.currentTimeMillis() / 1000 + 365L * 24 * 3600

    private fun spoofedUserAgent(): String =
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
}