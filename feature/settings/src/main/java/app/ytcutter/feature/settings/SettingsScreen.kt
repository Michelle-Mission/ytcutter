package app.ytcutter.feature.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.ytcutter.core.designsystem.ScreenScaffold
import app.ytcutter.core.designsystem.SectionHeader
import app.ytcutter.core.designsystem.SettingsRow
import app.ytcutter.core.designsystem.UpdateBanner
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytcutter.core.data.CrashLog
import app.ytcutter.core.data.EngineState
import app.ytcutter.core.data.YtDlpRepo
import app.ytcutter.core.data.cookies.CookiePrefs
import app.ytcutter.core.database.CookiesRepo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    appVersion: String,
    updateVersion: String?,
    onOpenUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenComponents: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenChangelog: () -> Unit,
    updateSize: Long? = null,
    updateStatus: String? = null,
    updateProgress: Float? = null,
    updateBusy: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cookieEntries by CookiesRepo.get(context).entries.collectAsStateWithLifecycle()
    val cookieOn = CookiePrefs.useCookies(context)
    val activeCookies = cookieEntries.count { it.enabled }

    val lastCrash = remember { CrashLog.last(context) }
    var crashVisible by remember { mutableStateOf(lastCrash != null) }

    ScreenScaffold(title = "Pengaturan") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp)
        ) {
        if (updateVersion != null) {
            UpdateBanner(
                version = updateVersion,
                onOpen = onOpenUpdate,
                onDismiss = onDismissUpdate,
                sizeBytes = updateSize,
                status = updateStatus,
                progress = updateProgress,
                busy = updateBusy,
                action = if (updateBusy) "Memproses" else "Perbarui"
            )
            Spacer(Modifier.height(14.dp))
        }

        CategoryLabel("Pembaruan")

        SettingsRow(
            icon = Icons.Default.SystemUpdate,
            title = "Komponen",
            subtitle = "Versi aplikasi, yt-dlp, dan pemeriksaan pembaruan",
            onClick = onOpenComponents,
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        SettingsRow(
            icon = Icons.Default.History,
            title = "Changelog",
            subtitle = "Riwayat perubahan tiap build",
            onClick = onOpenChangelog
        )

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

        Spacer(Modifier.height(18.dp))
        CategoryLabel("Cookie")

        SettingsRow(
            icon = Icons.Default.Cookie,
            title = if (activeCookies > 0) "$activeCookies cookie aktif" else "Belum ada cookie",
            subtitle = if (cookieOn) "Dipakai saat mengunduh" else "Tidak dikirim saat mengunduh",
            onClick = onOpenCookies,
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Spacer(Modifier.height(18.dp))
        CategoryLabel("Tentang")
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAbout)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("YT Cutter", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Versi $appVersion · arm64-v8a · GPL-3.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }
}

@Composable
private fun CategoryLabel(text: String) = SectionHeader(text)

