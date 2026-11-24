package app.ytcutter.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytcutter.core.designsystem.ScreenScaffold
import app.ytcutter.core.designsystem.SectionHeader
import app.ytcutter.core.designsystem.Spacing
import app.ytcutter.core.model.ChangeEntry

private val fallbackChanges = listOf(
    ChangeEntry(
        "1.0.0",
        listOf(
            "Rilis pertama YT Cutter.",
            "Potong dan unduh video YouTube tanpa mengunduh bagian yang tidak dipakai.",
            "Update delta: unduhan kecil, APK penuh hanya sebagai cadangan.",
            "Konfigurasi jarak jauh: pengumuman, maintenance, dan feature flag dari GitHub Pages.",
            "Changelog, status komponen, dan pemeriksaan pembaruan di dalam aplikasi.",
            "Dukungan cookie YouTube untuk video yang butuh login."
        )
    )
)
@Composable
fun ChangelogScreen(
    remoteEntries: suspend () -> List<ChangeEntry>? = { null },
    onBack: () -> Unit
) {
    val changes by produceState(fallbackChanges, remoteEntries) {
        value = remoteEntries() ?: fallbackChanges
    }
    ScreenScaffold(title = "Changelog", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            changes.forEach { entry ->
                SectionHeader(entry.label)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    entry.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}