package app.ytcutter.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.ytcutter.data.CookieStore
import app.ytcutter.data.YtDlpRepo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var hasCookies by remember { mutableStateOf(CookieStore.path(context) != null) }

    LaunchedEffect(Unit) {
        version = YtDlpRepo.versionName(context)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = CookieStore.importFrom(context, uri)
            hasCookies = CookieStore.path(context) != null
            message = if (ok) "Cookies diimpor" else "File cookies tidak terbaca"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Kembali") }
            Spacer(Modifier.width(12.dp))
            Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(24.dp))
        Text("Mesin", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Text("yt-dlp ${version ?: "…"}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bila YouTube mengubah sesuatu, versi terbaru biasanya memperbaikinya.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                message = null
                scope.launch {
                    message = YtDlpRepo.update(context)
                    version = YtDlpRepo.versionName(context)
                    busy = false
                }
            }
        ) { Text(if (busy) "Memperbarui…" else "Update yt-dlp") }

        if (busy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        Text("Cookies YouTube", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Dipakai untuk video private, age-restricted, atau saat YouTube minta verifikasi. " +
                "Ekspor cookies.txt dari browser (ekstensi \"Get cookies.txt\"), lalu impor di sini. " +
                "Cookies = akses akunmu: simpan hanya di perangkat ini.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            Button(onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }) {
                Text("Impor cookies.txt")
            }
            if (hasCookies) {
                OutlinedButton(onClick = {
                    CookieStore.clear(context)
                    hasCookies = false
                    message = "Cookies dihapus"
                }) { Text("Hapus") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasCookies) "Cookies aktif." else "Belum ada cookies.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }
    }
}