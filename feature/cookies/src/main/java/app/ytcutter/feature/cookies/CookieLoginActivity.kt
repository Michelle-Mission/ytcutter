package app.ytcutter.feature.cookies

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.ytcutter.core.common.AuthCookie
import app.ytcutter.core.data.cookies.CookiePrefs
import app.ytcutter.core.data.cookies.WebViewCookieReader
import app.ytcutter.core.database.CookiesRepo
import app.ytcutter.core.designsystem.theme.YtCutterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CookieLoginActivity : ComponentActivity() {

    private var webView: WebView? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var saving by mutableStateOf(false)
    private var autoSaveStarted = false
    private var statusText by mutableStateOf("Memeriksa cookie...")
    private var targetUrl by mutableStateOf("https://m.youtube.com/")
    private var description by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUrl = intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() } ?: targetUrl
        description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()

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
                                Text("Ambil cookie", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Login di halaman ini; keluar otomatis setelah login terdeteksi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { saveAndFinish() }, enabled = !saving) {
                                Text(if (saving) "Menyimpan..." else "Selesai")
                            }
                        }
                        if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                        AndroidView(
                            modifier = Modifier.weight(1f),
                            factory = { context ->
                                WebView(context).apply {
                                    webView = this
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            request: WebResourceRequest
                                        ): WebResourceResponse? = null

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            inspectCookies()
                                        }
                                    }
                                    loadUrl(targetUrl)
                                }
                            }
                        )
                        Text(
                            if (saving) "Menyimpan cookie..." else statusText,
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

    /** Status dihitung dari DB WebView (termasuk cookie HttpOnly), bukan header request. */
    private fun inspectCookies() {
        if (saving) return
        scope.launch {
            CookieManager.getInstance().flush()
            val content = withContext(Dispatchers.IO) { WebViewCookieReader.export(this@CookieLoginActivity) }
            val lines = content?.lines().orEmpty()
            val count = lines.count { it.isNotBlank() && !it.startsWith("# Netscape") }
            val loggedIn = AuthCookie.isLoggedIn(lines)
            statusText = when {
                loggedIn -> "Login terdeteksi — menyimpan cookie..."
                count > 0 -> "Cookie ada ($count) · tekan Selesai untuk menyimpan"
                else -> "Belum ada cookie — login dulu"
            }
            if (loggedIn && !autoSaveStarted) {
                autoSaveStarted = true
                delay(800)
                saveAndFinish()
            }
        }
    }

    private fun saveAndFinish() {
        if (saving) return
        saving = true
        scope.launch {
            CookieManager.getInstance().flush()
            val content = withContext(Dispatchers.IO) { WebViewCookieReader.export(this@CookieLoginActivity) }
            if (content == null) {
                saving = false
                Toast.makeText(this@CookieLoginActivity, "Belum ada cookie ditemukan", Toast.LENGTH_LONG).show()
                return@launch
            }
            val repo = CookiesRepo.get(this@CookieLoginActivity)
            val count = content.lines().count { it.isNotBlank() && !it.startsWith("# Netscape") }
            repo.insert(targetUrl, description.ifBlank { targetUrl }, content)
            CookiePrefs.setUseCookies(this@CookieLoginActivity, true)
            setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT_COUNT, count))
            Toast.makeText(this@CookieLoginActivity, "Cookie tersimpan ($count cookie)", Toast.LENGTH_SHORT).show()
            webView?.stopLoading()
            finish()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_RESULT_COUNT = "count"
    }
}