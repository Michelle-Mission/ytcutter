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
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    private var webView: WebView? = null
    private var cookieCount by mutableIntStateOf(0)
    private var saving by mutableStateOf(false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndFinish()
            }
        })

        setContent {
            YtCutterTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { saveAndFinish() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali & simpan"
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Login YouTube", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Login, lalu tutup halaman ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { saveAndFinish() },
                                enabled = !saving
                            ) { Text(if (saving) "Menyimpan…" else "Selesai") }
                        }
                        if (saving) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        AndroidView(
                            modifier = Modifier.weight(1f),
                            factory = { context ->
                                WebView(context).apply {
                                    webView = this
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
                                            captureHeaders(request)
                                            return null
                                        }
                                    }
                                    loadUrl("https://m.youtube.com/")
                                }
                            }
                        )
                        Text(
                            when {
                                saving -> "Menyimpan cookies…"
                                cookieCount > 0 -> "$cookieCount cookies tertangkap · tutup untuk menyimpan"
                                else -> "Belum ada cookies — login dulu"
                            },
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

    private fun captureHeaders(request: WebResourceRequest) {
        val host = request.url.host ?: return
        val header = request.requestHeaders?.get("Cookie") ?: return
        val domain = when {
            host.endsWith("youtube.com") -> ".youtube.com"
            host.endsWith("google.com") -> ".google.com"
            else -> return
        }
        CookieExport.fromCookieHeader(header, domain, expiry())
            .forEach { captured["${it.domain}|${it.name}"] = it }
        cookieCount = captured.count { it.key.startsWith(".youtube.com") }
    }

    private fun saveAndFinish() {
        if (saving) return
        saving = true
        val manager = CookieManager.getInstance()
        manager.flush()
        listOf(
            ".youtube.com" to "https://www.youtube.com",
            ".google.com" to "https://accounts.google.com"
        ).forEach { (domain, url) ->
            manager.getCookie(url)?.let { header ->
                CookieExport.fromCookieHeader(header, domain, expiry())
                    .forEach { captured["${it.domain}|${it.name}"] = it }
            }
        }
        if (captured.isNotEmpty()) {
            CookieStore.saveText(this, CookieExport.toNetscape(captured.values))
            setResult(RESULT_OK)
        }
        webView?.stopLoading()
        finish()
    }

    private fun expiry(): Long = System.currentTimeMillis() / 1000 + 365L * 24 * 3600

    private fun spoofedUserAgent(): String =
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
}