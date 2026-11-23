package app.ytcutter

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytcutter.core.UrlValidator
import app.ytcutter.data.CrashLog
import app.ytcutter.data.DownloadRequest
import app.ytcutter.data.Action
import app.ytcutter.data.history.HistoryRepo
import app.ytcutter.data.Downloads
import app.ytcutter.data.EngineState
import app.ytcutter.data.ErrorMapper
import app.ytcutter.data.InfoCache
import app.ytcutter.data.YouTubeInfo
import app.ytcutter.data.MetadataParser
import app.ytcutter.data.NewPipeMapper
import app.ytcutter.data.YtDlpRepo
import app.ytcutter.ui.DownloadsScreen
import app.ytcutter.ui.FormatSheet
import app.ytcutter.ui.HomeScreen
import app.ytcutter.ui.HomeState
import app.ytcutter.ui.SettingsScreen
import app.ytcutter.ui.components.AuroraBackdrop
import app.ytcutter.ui.theme.YtCutterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(applicationContext)
        setContent {
            YtCutterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine by YtDlpRepo.state.collectAsStateWithLifecycle()

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val crashText = remember { CrashLog.last(context.applicationContext) }
    var showCrash by remember { mutableStateOf(crashText != null) }

    LaunchedEffect(Unit) {
        CrashLog.markBooting(context.applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Mesin disiapkan di latar; UI langsung tampil.
        val init = YtDlpRepo.ensureInit(context.applicationContext)
        if (init.isSuccess) CrashLog.markBooted(context.applicationContext)
    }

    when (val state = engine) {
        EngineState.Ready, EngineState.Starting -> MainContent()
        is EngineState.Failed -> BootScreen(
            error = state.message,
            crash = state.detail ?: crashText,
            onShareCrash = {
                CrashLog.share(context, state.detail ?: crashText.orEmpty())
            },
            onClearCrash = {
                CrashLog.clear(context.applicationContext)
                showCrash = false
            },
            onRetry = {
                scope.launch {
                    CrashLog.markBooting(context.applicationContext)
                    val retry = YtDlpRepo.ensureInit(context.applicationContext)
                    if (retry.isSuccess) CrashLog.markBooted(context.applicationContext)
                }
            }
        )
    }
}

@Composable
private fun BootScreen(
    error: String? = null,
    crash: String? = null,
    onRetry: (() -> Unit)? = null,
    onShareCrash: (() -> Unit)? = null,
    onClearCrash: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize()) {
        AuroraBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    if (error == null) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (error == null) Icons.Default.Movie else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (error == null) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (error == null) "Menyiapkan mesin…" else "Mesin gagal disiapkan",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            error ?: "Pertama kali bisa butuh beberapa detik.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (error != null && onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry, shape = MaterialTheme.shapes.medium) { Text("Coba lagi") }
        } else {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
        }
        if (crash != null) {
            val clipboard = LocalClipboardManager.current
            Spacer(Modifier.height(22.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Detail teknis",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            crash.take(3000),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Row {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(crash)) }) {
                            Text("Salin")
                        }
                        TextButton(onClick = { onShareCrash?.invoke() }) { Text("Bagikan") }
                        TextButton(onClick = { onClearCrash?.invoke() }) { Text("Hapus") }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var homeState by remember { mutableStateOf<HomeState>(HomeState.Idle) }

    LaunchedEffect(Unit) {
        Downloads.resumePending(context.applicationContext)
        // beri waktu boot; update tidak boleh ikut memperlambat start
        kotlinx.coroutines.delay(12_000)
        val busy = HistoryRepo.get(context.applicationContext).downloads.value
            .any { it.status == app.ytcutter.data.history.DownloadStatus.RUNNING }
        if (!busy) YtDlpRepo.autoUpdateOnce(context.applicationContext)
    }
    var tab by remember { mutableIntStateOf(0) }
    var showFormat by remember { mutableStateOf(false) }
    val history by HistoryRepo.get(context.applicationContext).downloads.collectAsStateWithLifecycle()
    var retryAfterLogin by remember { mutableStateOf<Long?>(null) }
    val cookieLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = retryAfterLogin
        retryAfterLogin = null
        if (id != null && result.resultCode == android.app.Activity.RESULT_OK) {
            Downloads.retry(context, id)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text("Unduhan") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Pengaturan") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    state = homeState,
                    onSubmit = { url ->
                        if (!UrlValidator.isYouTube(url)) {
                            homeState = HomeState.Error("Bukan URL YouTube. Tempel tautan video.")
                        } else {
                            homeState = HomeState.Loading
                            scope.launch {
                                val fast = YouTubeInfo.fetch(context, url).getOrNull()
                                    ?.takeIf { NewPipeMapper.isUsable(it) }
                                if (fast != null) {
                                    homeState = HomeState.Loaded(
                                        fast,
                                        url,
                                        InfoCache.existingPath(context.cacheDir, url)
                                    )
                                    scope.launch {
                                        val meta = YtDlpRepo.json(context, url)
                                        val path = meta.json?.let { InfoCache.save(context, url, it) }
                                        val current = homeState
                                        if (path != null && current is HomeState.Loaded && current.url == url) {
                                            homeState = current.copy(infoJson = path)
                                        }
                                    }
                                } else {
                                    val result = YtDlpRepo.json(context, url)
                                    val info = result.json?.let { MetadataParser.parse(it) }
                                    homeState = when {
                                        info == null -> HomeState.Error(
                                            ErrorMapper.map(result.error ?: "").message
                                        )
                                        info.heights.isEmpty() && !info.hasAudio -> HomeState.Error(
                                            "URL ini bukan video tunggal (playlist/kanal). " +
                                                "Tempel tautan video."
                                        )
                                        else -> HomeState.Loaded(
                                            info,
                                            url,
                                            result.json?.let { InfoCache.save(context, url, it) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onChooseFormat = { showFormat = true }
                )
                1 -> DownloadsScreen(
                    rows = history,
                    onCancel = { Downloads.cancel(context, it) },
                    onRetry = { row ->
                        if (row.errorAction == Action.IMPORT_COOKIES) {
                            retryAfterLogin = row.id
                            cookieLauncher.launch(
                                Intent(context, app.ytcutter.ui.CookieLoginActivity::class.java)
                            )
                        } else {
                            Downloads.retry(context, row.id)
                        }
                    },
                    onDelete = { HistoryRepo.get(context).delete(it) },
                    onOpen = { row ->
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(android.net.Uri.parse(row.uri), "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            )
                        }
                    },
                    onShare = { row ->
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "video/*"
                                        putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(row.uri))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    },
                                    "Bagikan video"
                                )
                            )
                        }
                    }
                )
                else -> SettingsScreen(engine = EngineState.Ready)
            }
        }
    }

    if (showFormat) {
        val loaded = homeState as? HomeState.Loaded
        if (loaded != null) {
            FormatSheet(
                info = loaded.info,
                onDismiss = { showFormat = false },
                onStart = { quality, start, end ->
                    showFormat = false
                    val started = Downloads.enqueue(
                        context,
                        DownloadRequest(loaded.url, quality, start, end, loaded.infoJson),
                        loaded.info.title
                    )
                    if (!started) {
                        homeState = HomeState.Error(
                            "Masih ada unduhan berjalan. Tunggu atau batalkan dulu."
                        )
                    } else {
                        tab = 1
                    }
                }
            )
        }
    }
}