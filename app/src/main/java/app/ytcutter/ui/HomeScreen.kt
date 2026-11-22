package app.ytcutter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytcutter.core.TimeCode
import app.ytcutter.data.VideoInfo
import coil3.compose.AsyncImage

sealed interface HomeState {
    data object Idle : HomeState
    data object Loading : HomeState
    data class Error(val message: String) : HomeState
    data class Loaded(val info: VideoInfo, val url: String) : HomeState
}


@Composable
fun HomeScreen(
    state: HomeState,
    onSubmit: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onChooseFormat: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "YT Cutter",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("URL YouTube") },
            placeholder = { Text("https://youtu.be/…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = state !is HomeState.Loading,
                onClick = {
                    clipboard.getText()?.text?.let {
                        if (it.isNotBlank()) text = it.trim()
                    }
                }
            ) { Text("Tempel") }

            Button(
                enabled = text.isNotBlank() && state !is HomeState.Loading,
                onClick = { onSubmit(text.trim()) },
                modifier = Modifier.weight(1f)
            ) { Text("Ambil info") }
        }

        Spacer(Modifier.height(20.dp))

        when (state) {
            HomeState.Idle -> Text(
                "Tempel tautan video, lalu tandai detik awal dan akhir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HomeState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(Modifier.width(12.dp))
                Text("Mengambil info video…")
            }

            is HomeState.Error -> Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Gagal", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is HomeState.Loaded -> VideoCard(state.info, onChooseFormat)
        }
    }
}

@Composable
private fun VideoCard(info: VideoInfo, onChooseFormat: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            info.thumbnailUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    info.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    TimeCode.format(info.durationSec),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onChooseFormat, modifier = Modifier.fillMaxWidth()) {
                    Text("Pilih resolusi & rentang")
                }
            }
        }
    }
}