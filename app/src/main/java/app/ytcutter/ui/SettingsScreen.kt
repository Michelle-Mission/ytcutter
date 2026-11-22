package app.ytcutter.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.ytcutter.data.CookieStore
import app.ytcutter.data.CrashLog
import app.ytcutter.data.EngineState
import app.ytcutter.data.YtDlpRepo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(engine: EngineState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cookieCount by remember { mutableStateOf(countCookies(context)) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    val lastCrash = remember { CrashLog.last(context) }
    var crashVisible by remember { mutableStateOf(lastCrash != null) }
    var updating by remember { mutableStateOf(false) }

    val version by produceState(initialValue = "...", engine) {
        value = if (engine is EngineState.Ready) {
            YtDlpRepo.versionName(context) ?: "tidak diketahui"
        } else {
            "belum siap"
        }
    }

    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cookieCount = countCookies(context)
            updateMessage = "Cookies tersimpan"
        } else {
            updateMessage = "Login dibatalkan atau tidak ada cookies"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = CookieStore.importFrom(context, uri)
        cookieCount = countCookies(context)
        updateMessage = if (ok) "Cookies diimpor" else "File cookies tidak valid"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mesin yt-dlp", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    when (engine) {
                        EngineState.Ready -> StatePill("Siap")
                        EngineState.Starting -> StatePill("Menyiapkan")
                        is EngineState.Failed -> StatePill("Error", error = true)
                    }
                }
                updateMessage?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    enabled = engine is EngineState.Ready && !updating,
                    onClick = {
                        scope.launch {
                            updating = true
                            updateMessage = "Memeriksa pembaruan..."
                            updateMessage = YtDlpRepo.update(context)
                            YtDlpRepo.markUpdateChecked(context)
                            updating = false
                        }
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    if (updating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Perbarui yt-dlp")
                }
            }
        }

        if (crashVisible && lastCrash != null) {
            Spacer(Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Log crash terakhir",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        lastCrash.lineSequence().take(6).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Row {
                        TextButton(onClick = { CrashLog.share(context, lastCrash) }) { Text("Bagikan") }
                        TextButton(onClick = {
                            CrashLog.clear(context)
                            crashVisible = false
                        }) { Text("Hapus") }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Cookie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cookies YouTube", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (cookieCount > 0) "$cookieCount cookies aktif. Video yang butuh login bisa diunduh." +
                        " Cookies disimpan hanya di perangkat ini."
                    else "Belum ada cookies. Video tertentu butuh login (usia, privasi, atau anti-bot).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            loginLauncher.launch(
                                Intent(context, CookieLoginActivity::class.java)
                            )
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Login YouTube")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Impor file")
                    }
                }
                if (cookieCount > 0) {
                    TextButton(onClick = {
                        CookieStore.clear(context)
                        cookieCount = 0
                        updateMessage = "Cookies dihapus"
                    }) { Text("Hapus cookies") }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "YT Cutter · 0.1.9 · arm64-v8a",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun StatePill(text: String, error: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun countCookies(context: android.content.Context): Int {
    val text = CookieStore.load(context) ?: return 0
    return text.lineSequence().count { it.isNotBlank() && !it.startsWith("#") }
}