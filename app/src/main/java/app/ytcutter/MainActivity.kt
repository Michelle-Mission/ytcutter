package app.ytcutter

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ytcutter.core.UrlValidator
import app.ytcutter.data.DownloadRequest
import app.ytcutter.data.Downloads
import app.ytcutter.data.MetadataParser
import app.ytcutter.data.YtDlpRepo
import app.ytcutter.ui.DownloadsScreen
import app.ytcutter.ui.FormatSheet
import app.ytcutter.ui.HomeScreen
import app.ytcutter.ui.HomeState
import app.ytcutter.ui.SettingsScreen
import app.ytcutter.ui.theme.YtCutterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    var homeState by remember { mutableStateOf<HomeState>(HomeState.Idle) }
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }
    val downloadState by Downloads.state.collectAsStateWithLifecycle()

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        runCatching { YtDlpRepo.init(context.applicationContext) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
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
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (tab == 0) {
                HomeScreen(
                    state = homeState,
                    onSubmit = { url ->
                        if (!UrlValidator.isYouTube(url)) {
                            homeState = HomeState.Error("Bukan URL YouTube. Tempel tautan video.")
                        } else {
                            homeState = HomeState.Loading
                            scope.launch {
                                val json = YtDlpRepo.json(context, url)
                                val info = json?.let { MetadataParser.parse(it) }
                                homeState = if (info != null) {
                                    HomeState.Loaded(info, url)
                                } else {
                                    HomeState.Error(
                                        "Gagal mengambil info video. Coba lagi, atau update yt-dlp " +
                                            "di Pengaturan bila terus gagal."
                                    )
                                }
                            }
                        }
                    },
                    onOpenSettings = { showSettings = true },
                    onChooseFormat = { showFormat = true }
                )
            } else {
                DownloadsScreen(
                    state = downloadState,
                    onCancel = { Downloads.cancel() }
                )
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
                    Downloads.start(
                        context,
                        DownloadRequest(loaded.url, quality, start, end),
                        loaded.info.title
                    )
                    tab = 1
                }
            )
        }
    }
}