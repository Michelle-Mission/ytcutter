package app.ytcutter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytcutter.core.FormatMapper
import app.ytcutter.core.Quality
import app.ytcutter.core.TimeCode
import app.ytcutter.data.VideoInfo
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSheet(
    info: VideoInfo,
    onDismiss: () -> Unit,
    onStart: (Quality, Int, Int) -> Unit
) {
    val options = remember(info) { FormatMapper.options(info.heights, info.hasAudio) }
    val duration = info.durationSec.coerceAtLeast(1)
    val focus = LocalFocusManager.current

    var quality by remember { mutableStateOf(options.firstOrNull()) }
    var start by remember { mutableIntStateOf(0) }
    var end by remember { mutableIntStateOf(minOf(60, duration)) }
    var startText by remember { mutableStateOf(TimeCode.format(start)) }
    var endText by remember { mutableStateOf(TimeCode.format(end)) }

    val valid = quality != null && end > start + 1

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                info.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Durasi ${TimeCode.format(info.durationSec)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Text("Resolusi", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    FilterChip(
                        selected = option == quality,
                        onClick = { quality = option },
                        label = { Text(option.label) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Rentang", style = MaterialTheme.typography.labelLarge)
            RangeSlider(
                value = start.toFloat()..end.toFloat(),
                onValueChange = { range ->
                    start = range.start.roundToInt().coerceIn(0, duration - 2)
                    end = range.endInclusive.roundToInt().coerceIn(start + 2, duration)
                    startText = TimeCode.format(start)
                    endText = TimeCode.format(end)
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Mulai") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (!state.isFocused) {
                                TimeCode.parse(startText)?.let { parsed ->
                                    start = parsed.coerceIn(0, (end - 2).coerceAtLeast(0))
                                    startText = TimeCode.format(start)
                                } ?: run { startText = TimeCode.format(start) }
                            }
                        }
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("Akhir") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (!state.isFocused) {
                                TimeCode.parse(endText)?.let { parsed ->
                                    end = parsed.coerceIn(start + 2, duration)
                                    endText = TimeCode.format(end)
                                } ?: run { endText = TimeCode.format(end) }
                            }
                        }
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                enabled = valid,
                onClick = {
                    val q = quality ?: return@Button
                    focus.clearFocus()
                    onStart(q, start, end)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Ambil ${TimeCode.format((end - start).coerceAtLeast(0))} · " +
                        (quality?.label ?: "-")
                )
            }
        }
    }
}