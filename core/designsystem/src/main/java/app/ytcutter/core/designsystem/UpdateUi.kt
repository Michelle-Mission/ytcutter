package app.ytcutter.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytcutter.core.model.CookieStatus

@Composable
fun UpdateBanner(
    version: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sizeBytes: Long? = null,
    status: String? = null,
    progress: Float? = null,
    busy: Boolean = false,
    action: String = "Perbarui"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = buildString {
                        append("YT Cutter $version tersedia")
                        if (sizeBytes != null) {
                            append(" · ")
                            append("%.1f MB".format(sizeBytes / 1024.0 / 1024.0))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (status != null) {
                    Text(
                        text = if (progress != null) "$status ${(progress * 100).toInt()}%" else status,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
            }
            TextButton(onClick = onOpen, enabled = !busy) { Text(action) }
            IconButton(onClick = onDismiss, enabled = !busy) {
                Icon(Icons.Default.Close, contentDescription = "Nanti")
            }
        }
    }
}

@Composable
fun UpdateCard(
    version: String,
    notes: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Versi aplikasi $version", style = MaterialTheme.typography.titleSmall)
            if (notes.isNotBlank()) {
                Text(
                    notes.take(400),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpen) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Buka halaman rilis", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun CookieStatusLine(
    status: CookieStatus,
    count: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        CookieStatus.LOGGED_IN -> "$count cookie · akun YouTube terdeteksi" to
            MaterialTheme.colorScheme.primary
        CookieStatus.NOT_LOGGED_IN -> "$count cookie · belum login YouTube" to
            MaterialTheme.colorScheme.error
        CookieStatus.UNKNOWN -> "$count cookie · status belum bisa dipastikan" to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)
}