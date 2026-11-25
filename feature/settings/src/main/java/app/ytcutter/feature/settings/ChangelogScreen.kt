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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytcutter.core.designsystem.ScreenScaffold
import app.ytcutter.core.designsystem.SectionHeader
import app.ytcutter.core.designsystem.Spacing

private data class ChangeEntry(val label: String, val items: List<String>)

private val changes = listOf(
    ChangeEntry(
        "1.0.0",
        listOf(
            "Rilis pertama YT Cutter.",
            "Potong dan unduh hanya rentang waktu yang dipilih, langsung ke galeri.",
            "Pilihan tinggi video dan audio saja.",
            "Cookie YouTube untuk video yang butuh login, plus poToken.",
            "Unduhan tahan gangguan: aria2c multi-koneksi, retry berlapis, " +
                "fallback ke format progresif.",
            "Status bertahap ala yt-dlp dengan progres dari byte/estimasi/fragmen.",
            "Pembaruan delta: unduhan kecil, APK penuh hanya sebagai cadangan.",
            "yt-dlp diperbarui dari dalam aplikasi dengan verifikasi SHA-256.",
            "FFmpeg, Python, dan aria2c menyatu di APK.",
            "Performa menyesuaikan perangkat kecil; info video dimuat lebih awal."
        )
    )
)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
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