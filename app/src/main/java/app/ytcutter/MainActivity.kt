package app.ytcutter

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ytcutter.core.common.UrlValidator
import app.ytcutter.core.data.CrashLog
import app.ytcutter.core.data.Downloads
import app.ytcutter.core.data.EngineState
import app.ytcutter.core.data.ErrorMapper
import app.ytcutter.core.data.InfoCache
import app.ytcutter.core.data.MetadataParser
import app.ytcutter.core.data.NewPipeMapper
import app.ytcutter.core.data.YtDlpRepo
import app.ytcutter.core.data.YouTubeInfo
import app.ytcutter.core.data.update.ApkInstaller
import app.ytcutter.core.data.update.AppUpdater
import app.ytcutter.core.data.update.InstallResult
import app.ytcutter.core.database.HistoryRepo
import app.ytcutter.core.designsystem.AuroraBackdrop
import app.ytcutter.core.designsystem.theme.YtCutterTheme
import app.ytcutter.core.model.Action
import app.ytcutter.core.model.AppUpdate
import app.ytcutter.core.model.ComponentInfo
import app.ytcutter.core.model.DownloadRequest
import app.ytcutter.core.model.DownloadStatus
import app.ytcutter.core.navigation.Route
import app.ytcutter.core.navigation.Routes
import app.ytcutter.feature.cookies.CookieLoginActivity
import app.ytcutter.feature.cookies.CookieScreen as CookiesFeatureScreen
import app.ytcutter.feature.downloads.DownloadsScreen
import app.ytcutter.feature.home.FormatSheet
import app.ytcutter.feature.home.HomeScreen
import app.ytcutter.feature.home.HomeState
import app.ytcutter.feature.settings.AboutScreen
import app.ytcutter.feature.settings.ChangelogScreen
import app.ytcutter.feature.settings.ComponentsScreen
import app.ytcutter.feature.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
        Downloads.resumePending(context.applicationContext)
        val init = YtDlpRepo.ensureInit(context.applicationContext)
        if (init.isSuccess) CrashLog.markBooted(context.applicationContext)
    }

    when (val state = engine) {
        EngineState.Ready, EngineState.Starting -> AppNavHost()
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
private fun AppNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = !Routes.hidesBottomBar(currentRoute)

    var update by remember { mutableStateOf<AppUpdate?>(null) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var updateProgress by remember { mutableStateOf<Float?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    val updateScope = rememberCoroutineScope()

    fun startUpdate() {
        val info = update ?: return
        if (!ApkInstaller.canInstall(context)) {
            ApkInstaller.openPermissionSettings(context)
            updateStatus = "Izinkan pemasangan dari aplikasi ini, lalu tekan Perbarui lagi."
            return
        }
        if (updateBusy) return
        updateBusy = true
        updateProgress = null
        updateStatus = "Menyiapkan pembaruan"
        updateScope.launch {
            runCatching {
                val apk = AppUpdater(context).prepare(
                    info,
                    onStatus = { updateStatus = it },
                    onProgress = { updateProgress = it }
                )
                updateStatus = "Memasang"
                updateProgress = null
                when (val result = ApkInstaller.install(context, apk)) {
                    InstallResult.Installed -> updateStatus = "Pembaruan terpasang"
                    InstallResult.Cancelled -> updateStatus = "Pemasangan dibatalkan"
                    InstallResult.NeedsPermission -> {
                        updateStatus = "Izinkan pemasangan dari aplikasi ini"
                        ApkInstaller.openPermissionSettings(context)
                    }
                    is InstallResult.Failed -> updateStatus = result.message
                }
            }.onFailure {
                updateStatus = it.message ?: "Pembaruan gagal"
            }
            updateBusy = false
        }
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Route.Home.path) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.Home.path) {
                HomeRoute(navController)
            }
            composable(Route.Downloads.path) {
                DownloadsRoute(navController)
            }
            composable(Route.Settings.path) {
                SettingsRoute(
                    updateVersion = update?.versionName,
                    onOpenUpdate = { startUpdate() },
                    onDismissUpdate = {
                        update?.let {
                            AppGraph.updatePrefs(context).ignoredVersion = it.versionCode.toString()
                        }
                        update = null
                        updateStatus = null
                    },
                    onOpenCookies = { navController.navigate(Route.Cookies.path) },
                    onOpenComponents = { navController.navigate(Route.Components.path) },
                    onOpenAbout = { navController.navigate(Route.About.path) },
                    onOpenChangelog = { navController.navigate(Route.Changelog.path) },
                    updateSize = update?.let { u -> u.patch?.asset?.size ?: u.apk.size },
                    updateStatus = updateStatus,
                    updateProgress = updateProgress,
                    updateBusy = updateBusy
                )
            }
            composable(Route.Components.path) {
                ComponentsRoute(navController)
            }
            composable(Route.Changelog.path) {
                ChangelogScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.About.path) {
                AboutScreen(
                    appVersion = AppGraph.appVersion(context),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.Cookies.path) {
                CookiesFeatureScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Route.Settings.path) return@LaunchedEffect
        val info = AppUpdater(context).check() ?: return@LaunchedEffect
        if (AppGraph.updatePrefs(context).ignoredVersion != info.versionCode.toString()) {
            update = info
        }
    }
}

@Composable
private fun HomeRoute(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var homeState by remember { mutableStateOf<HomeState>(HomeState.Idle) }
    var showFormat by remember { mutableStateOf(false) }

    HomeScreen(
        state = homeState,
        onSubmit = { url ->
            if (!UrlValidator.isYouTube(url)) {
                homeState = HomeState.Error("Bukan URL YouTube. Tempel tautan video.")
            } else {
                homeState = HomeState.Loading
                scope.launch {
                    val fast = withTimeoutOrNull(8_000) { YouTubeInfo.fetch(context, url) }
                        ?.getOrNull()
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
                        navController.navigate(Route.Downloads.path)
                    }
                }
            )
        }
    }
}

@Composable
private fun DownloadsRoute(navController: NavHostController) {
    val context = LocalContext.current
    val history by HistoryRepo.get(context.applicationContext).downloads
        .collectAsStateWithLifecycle()
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

    DownloadsScreen(
        rows = history,
        onCancel = { Downloads.cancel(context, it) },
        onRetry = { row ->
            if (row.errorAction == Action.IMPORT_COOKIES) {
                retryAfterLogin = row.id
                cookieLauncher.launch(Intent(context, CookieLoginActivity::class.java))
            } else {
                Downloads.retry(context, row.id)
            }
        },
        onDelete = { HistoryRepo.get(context.applicationContext).delete(it) },
        onOpen = { row ->
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(row.uri), "video/*")
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
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(row.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Bagikan video"
                    )
                )
            }
        }
    )
}

@Composable
private fun SettingsRoute(
    updateVersion: String?,
    onOpenUpdate: () -> Unit,
    onDismissUpdate: (String) -> Unit,
    onOpenCookies: () -> Unit,
    onOpenComponents: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenChangelog: () -> Unit,
    updateSize: Long?,
    updateStatus: String?,
    updateProgress: Float?,
    updateBusy: Boolean
) {
    val context = LocalContext.current
    SettingsScreen(
        appVersion = AppGraph.appVersion(context),
        updateVersion = updateVersion,
        onOpenUpdate = onOpenUpdate,
        onDismissUpdate = {
            if (updateVersion != null) onDismissUpdate(updateVersion)
        },
        onOpenCookies = onOpenCookies,
        onOpenComponents = onOpenComponents,
        onOpenAbout = onOpenAbout,
        onOpenChangelog = onOpenChangelog,
        updateSize = updateSize,
        updateStatus = updateStatus,
        updateProgress = updateProgress,
        updateBusy = updateBusy
    )
}

@Composable
private fun ComponentsRoute(navController: NavHostController) {
    val context = LocalContext.current
    var components by remember { mutableStateOf<List<ComponentInfo>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var releaseUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        busy = true
        components = AppGraph.componentRepository(context).components()
        releaseUrl = AppGraph.componentRepository(context).latestRelease()?.url
        busy = false
    }

    ComponentsScreen(
        components = components,
        busy = busy,
        message = message,
        onCheck = {
            busy = true
            val list = AppGraph.componentRepository(context).components()
            val release = AppGraph.componentRepository(context).latestRelease()
            components = list
            releaseUrl = release?.url
            busy = false
            if (release == null) "Tidak ada pembaruan tersedia" else "Versi ${release.version} tersedia"
        },
        onUpdateYtDlp = {
            busy = true
            val result = AppGraph.componentRepository(context).updateYtDlp()
            components = AppGraph.componentRepository(context).components()
            busy = false
            result
        },
        onOpenRelease = { url ->
            openUrl(context, url)
        },
        releaseUrl = releaseUrl,
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun AppBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        val items = listOf(
            Triple(Route.Home, Icons.Default.Home, "Beranda"),
            Triple(Route.Downloads, Icons.Default.Download, "Unduhan"),
            Triple(Route.Settings, Icons.Default.Settings, "Pengaturan")
        )
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                selected = currentRoute == route.path,
                onClick = { onNavigate(route.path) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) }
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                if (error == null) "Menyiapkan mesin..." else "Mesin gagal disiapkan",
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