package app.ytcutter.feature.cookies

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytcutter.core.data.cookies.CookieCheck
import app.ytcutter.core.designsystem.ScreenScaffold
import app.ytcutter.core.designsystem.StatusPill
import app.ytcutter.core.data.cookies.CookiePrefs
import app.ytcutter.core.database.CookieRow
import app.ytcutter.core.model.CookieStatus
import app.ytcutter.core.database.CookiesRepo
import app.ytcutter.core.common.NetscapeFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val repo = remember { CookiesRepo.get(context) }
    val entries by repo.entries.collectAsStateWithLifecycle()

    var useCookies by remember { mutableStateOf(CookiePrefs.useCookies(context)) }
    var showMenu by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<CookieRow?>(null) }
    var checkText by remember { mutableStateOf<String?>(null) }
    val dbCount = remember(entries, useCookies) { CookieCheck.count(context) }

    LaunchedEffect(Unit) { runProbe(context) { checkText = it } }

    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val count = result.data?.getIntExtra(CookieLoginActivity.EXTRA_RESULT_COUNT, 0) ?: 0
            useCookies = true
            if (count > 0) runProbe(context) { checkText = it }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(repo.buildEnabledText().toByteArray())
                }
            }.onSuccess { toast(context, "Cookie diekspor") }
                .onFailure { toast(context, "Gagal mengekspor cookie") }
        }
    }

    ScreenScaffold(
        title = "Cookie",
        onBack = onBack,
        actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Hapus semua cookie") },
                            onClick = {
                                showMenu = false
                                showDeleteAll = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Impor dari papan klip") },
                            onClick = {
                                showMenu = false
                                val body = NetscapeFile.parseClipboard(
                                    clipboard.getText()?.text.orEmpty()
                                )
                                if (body == null) {
                                    toast(context, "Papan klip tidak berisi cookie Netscape")
                                } else {
                                    repo.insert("", "Impor dari papan klip", body)
                                    CookiePrefs.setUseCookies(context, true)
                                    useCookies = true
                                    toast(context, "Cookie diimpor")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor ke papan klip") },
                            onClick = {
                                showMenu = false
                                clipboard.setText(AnnotatedString(repo.buildEnabledText()))
                                toast(context, "Cookie disalin")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor berkas") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("cookies.txt")
                            }
                        )
                    }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Pakai cookie", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Opsional. Untuk video usia, privat, anggota, atau cek bot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useCookies,
                    onCheckedChange = {
                        useCookies = it
                        CookiePrefs.setUseCookies(context, it)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAdd = true },
                    enabled = useCookies,
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cookie")
                }
                OutlinedButton(
                    onClick = { runProbe(context) { checkText = it } },
                    shape = MaterialTheme.shapes.small
                ) { Text("Cek cookie") }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("$dbCount cookie tersimpan")
                checkText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))
            if (entries.isEmpty()) {
                Text(
                    "Belum ada cookie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEach { row ->
                    CookieEntryCard(
                        row = row,
                        onToggle = { repo.toggle(row.id, it) },
                        onOpen = { detail = row }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAdd) {
        CookieFormDialog(
            title = "Ambil cookie",
            initialUrl = "https://",
            initialDescription = "",
            onDismiss = { showAdd = false },
            onConfirm = { url, desc ->
                showAdd = false
                loginLauncher.launch(
                    Intent(context, CookieLoginActivity::class.java)
                        .putExtra(CookieLoginActivity.EXTRA_URL, url)
                        .putExtra(CookieLoginActivity.EXTRA_DESCRIPTION, desc)
                )
            }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Hapus semua cookie?") },
            text = { Text("Semua entri cookie akan dihapus dari perangkat.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.deleteAll()
                    useCookies = false
                    CookiePrefs.setUseCookies(context, false)
                    showDeleteAll = false
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text("Batal") }
            }
        )
    }

    detail?.let { row ->
        CookieDetailDialog(
            row = row,
            onDismiss = { detail = null },
            onCopy = {
                clipboard.setText(AnnotatedString(NetscapeFile.HEADER + row.content))
                toast(context, "Cookie disalin")
            },
            onDelete = {
                repo.delete(row.id)
                detail = null
            },
            onUpdate = { url, desc ->
                repo.update(row.id, url, desc)
                detail = null
            }
        )
    }
}

@Composable
private fun CookieEntryCard(row: CookieRow, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) {
    Card(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.description.ifBlank { row.url }.ifBlank { "Cookie" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${row.content.lines().count { NetscapeFile.isCookieLine(it) }} cookie · ${row.url}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onOpen) { Text("Lihat") }
            Switch(checked = row.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun CookieFormDialog(
    title: String,
    initialUrl: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var desc by remember { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url.trim(), desc.trim()) },
                enabled = url.startsWith("http")
            ) { Text("Ambil cookie") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun CookieDetailDialog(
    row: CookieRow,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (String, String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(row.url) }
    var desc by remember { mutableStateOf(row.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.description.ifBlank { "Cookie" }) },
        text = {
            Column(Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                if (editing) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Deskripsi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    SelectionContainer {
                        Text(
                            row.content,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (editing) {
                TextButton(onClick = { onUpdate(url.trim(), desc.trim()) }) { Text("Simpan") }
            } else {
                TextButton(onClick = onCopy) { Text("Salin") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { editing = !editing }) {
                    Text(if (editing) "Batal ubah" else "Ubah")
                }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    )
}

private fun runProbe(context: android.content.Context, report: (String) -> Unit) {
    val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()
    )
    scope.launch {
        val result = CookieCheck.probe(context.applicationContext)
        report(
            when (result) {
                CookieStatus.LOGGED_IN -> "Cookie ada — login YouTube aktif"
                CookieStatus.NOT_LOGGED_IN -> "Cookie ada, tapi belum login"
                CookieStatus.UNKNOWN -> "Status cookie tidak diketahui"
            }
        )
    }
}

private fun toast(context: android.content.Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}