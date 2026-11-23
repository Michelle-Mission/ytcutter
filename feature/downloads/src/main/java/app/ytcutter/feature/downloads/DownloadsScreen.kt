package app.ytcutter.feature.downloads

import app.ytcutter.core.designsystem.ScreenHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytcutter.core.model.Action
import app.ytcutter.core.model.DownloadRow
import app.ytcutter.core.model.DownloadStatus
import app.ytcutter.core.database.HistoryRules
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(
    rows: List<DownloadRow>,
    onCancel: (Long) -> Unit,
    onRetry: (DownloadRow) -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (DownloadRow) -> Unit,
    onShare: (DownloadRow) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        ScreenHeader(
            title = "Unduhan",
            subtitle = "Hasil disimpan di galeri: Movies/YT Cutter (audio: Music/YT Cutter)",
            horizontalPadding = 0.dp
        )

        if (rows.isEmpty()) {
            EmptyDownloads()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows, key = { it.id }) { row ->
                    HistoryCard(
                        row = row,
                        onCancel = { onCancel(row.id) },
                        onRetry = { onRetry(row) },
                        onDelete = { onDelete(row.id) },
                        onOpen = { onOpen(row) },
                        onShare = { onShare(row) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    row: DownloadRow,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${row.qualityLabel} · ${rangeText(row)} · ${timeText(row.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                StatusPill(statusLabel(row.status), row.status == DownloadStatus.FAILED || row.status == DownloadStatus.INTERRUPTED)
            }

            when (row.status) {
                DownloadStatus.RUNNING -> {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { row.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${row.progress}% ${row.detail.orEmpty()}".trim(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCancel, shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Batal")
                    }
                }

                DownloadStatus.QUEUED -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Menunggu giliran...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCancel, shape = MaterialTheme.shapes.small) {
                        Text("Batalkan")
                    }
                }

                DownloadStatus.DONE -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpen, shape = MaterialTheme.shapes.small) { Text("Buka") }
                        OutlinedButton(onClick = onShare, shape = MaterialTheme.shapes.small) { Text("Bagikan") }
                        TextButton(onClick = onDelete) { Text("Hapus riwayat") }
                    }
                }

                else -> {
                    row.error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (HistoryRules.canRetry(row.status)) {
                            Button(onClick = onRetry, shape = MaterialTheme.shapes.small) {
                                Text(if (row.errorAction == Action.NONE) "Ulangi" else "Perbaiki & ulangi")
                            }
                        }
                        TextButton(onClick = onDelete) { Text("Hapus riwayat") }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.QUEUED -> "Antre"
    DownloadStatus.RUNNING -> "Berjalan"
    DownloadStatus.DONE -> "Selesai"
    DownloadStatus.FAILED -> "Gagal"
    DownloadStatus.CANCELED -> "Dibatalkan"
    DownloadStatus.INTERRUPTED -> "Terputus"
}

@Composable
private fun StatusPill(text: String, failed: Boolean) {
    app.ytcutter.core.designsystem.StatusPill(text = text, error = failed)
}

@Composable
private fun EmptyDownloads() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        Icon(
            Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Text("Belum ada unduhan", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tempel tautan YouTube di Beranda, pilih rentang, lalu mulai.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun rangeText(row: DownloadRow): String =
    if (row.startSec == 0 && row.endSec == 0) "penuh"
    else "${clock(row.startSec)}–${clock(row.endSec)}"

private fun clock(totalSeconds: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

private fun timeText(epochMs: Long): String =
    SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")).format(Date(epochMs))