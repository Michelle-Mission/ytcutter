package app.ytcutter.feature.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytcutter.core.designsystem.ScreenScaffold
import app.ytcutter.core.designsystem.SettingsRow
import app.ytcutter.core.model.ComponentInfo
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    components: List<ComponentInfo>,
    busy: Boolean,
    message: String?,
    onCheck: suspend () -> String,
    onUpdateYtDlp: suspend () -> String,
    onOpenRelease: (String) -> Unit,
    releaseUrl: String?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(message) }

    ScreenScaffold(title = "Komponen", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Hanya yt-dlp yang bisa diperbarui tanpa memasang APK baru.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            components.forEach { item ->
                Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.weight(1f))
                            Text(
                                item.version,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.name == "yt-dlp") {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { scope.launch { status = onUpdateYtDlp() } },
                                shape = MaterialTheme.shapes.small
                            ) { Text("Perbarui yt-dlp") }
                        }
                    }
                }
            }

            if (releaseUrl != null) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = { onOpenRelease(releaseUrl) },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka rilis terbaru")
                }
            }

            OutlinedButton(
                enabled = !busy,
                onClick = { scope.launch { status = onCheck() } },
                shape = MaterialTheme.shapes.small
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Periksa pembaruan")
            }

            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appVersion: String,
    onOpen: (String) -> Unit,
    onBack: () -> Unit
) {
    ScreenScaffold(title = "Tentang", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("YT Cutter", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Versi $appVersion · arm64-v8a · GPL-3.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Pengunduh video YouTube pribadi. Dibangun dengan Kotlin, Jetpack Compose, " +
                    "yt-dlp, dan NewPipe Extractor.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(6.dp))
            SettingsRow(
                icon = Icons.Default.Favorite,
                title = "Dukung lewat Trakteer",
                subtitle = "Tip sukarela untuk pengembang",
                onClick = { onOpen("https://trakteer.id/nurfilardi") },
                trailing = { OpenMark() }
            )
            SettingsRow(
                icon = Icons.Default.Person,
                title = "Pengembang",
                subtitle = "github.com/nurfilardi",
                onClick = { onOpen("https://github.com/nurfilardi") },
                trailing = { OpenMark() }
            )
            SettingsRow(
                icon = Icons.Default.Code,
                title = "Kode proyek",
                subtitle = "github.com/nurfilardi/ytcutter",
                onClick = { onOpen("https://github.com/nurfilardi/ytcutter") },
                trailing = { OpenMark() }
            )
            SettingsRow(
                icon = Icons.Default.Language,
                title = "Situs web",
                subtitle = "nurfilardi.github.io/ytcutter",
                onClick = { onOpen("https://nurfilardi.github.io/ytcutter/") },
                trailing = { OpenMark() }
            )
        }
    }
}

@Composable
private fun OpenMark() {
    Icon(
        Icons.Default.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
    )
}