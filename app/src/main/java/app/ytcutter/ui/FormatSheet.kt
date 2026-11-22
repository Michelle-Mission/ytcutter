package app.ytcutter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
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
import app.ytcutter.core.Range
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
    val initial = remember(duration) { Range.clamp(0, minOf(60, duration), duration) }
    var start by remember { mutableIntStateOf(initial.first) }
    var end by remember { mutableIntStateOf(initial.second) }
    var startText by remember { mutableStateOf(TimeCode.format(start)) }
    var endText by remember { mutableStateOf(TimeCode.format(end)) }

    val selectable = Range.isSelectable(duration)
    val valid = quality != null && selectable && end > start + 1
    val clipLength = (end - start).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
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
                "Durasi ${TimeCode.format(info.durationSec)} · ${options.size} pilihan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Resolusi")
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 20.dp)
            ) {
                items(options) { option ->
                    FilterChip(
                        selected = option == quality,
                        onClick = { quality = option },
                        label = {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = if (option == quality) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            null
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Rentang", Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        TimeCode.format(clipLength),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Geser dua titik, atau ketik waktu di bawah",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!selectable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Video terlalu pendek untuk dipotong (butuh minimal 3 detik).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            RangeSlider(
                value = start.toFloat()..end.toFloat(),
                enabled = selectable,
                onValueChange = { range ->
                    val clamped = Range.clamp(
                        range.start.roundToInt(),
                        range.endInclusive.roundToInt(),
                        duration
                    )
                    start = clamped.first
                    end = clamped.second
                    startText = TimeCode.format(start)
                    endText = TimeCode.format(end)
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("Mulai") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (!state.isFocused) {
                                val parsed = TimeCode.parse(startText)
                                if (parsed != null) {
                                    val clamped = Range.clamp(parsed, end, duration)
                                    start = clamped.first
                                    end = clamped.second
                                }
                                startText = TimeCode.format(start)
                                endText = TimeCode.format(end)
                            }
                        }
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("Akhir") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (!state.isFocused) {
                                val parsed = TimeCode.parse(endText)
                                if (parsed != null) {
                                    val clamped = Range.clamp(start, parsed, duration)
                                    start = clamped.first
                                    end = clamped.second
                                }
                                startText = TimeCode.format(start)
                                endText = TimeCode.format(end)
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
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ambil ${TimeCode.format(clipLength)} · ${quality?.label ?: "-"}")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}