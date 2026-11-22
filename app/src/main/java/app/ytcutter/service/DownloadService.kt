package app.ytcutter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.ytcutter.MainActivity
import app.ytcutter.core.Quality
import app.ytcutter.data.CookieStore
import app.ytcutter.data.DownloadArgs
import app.ytcutter.data.DownloadRequest
import app.ytcutter.data.DownloadState
import app.ytcutter.data.Downloads
import app.ytcutter.data.ErrorMapper
import app.ytcutter.data.FileNames
import app.ytcutter.data.MediaStorePublisher
import app.ytcutter.data.YtDlpRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    @Volatile
    private var runningProcessId: String? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Unduhan", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val safe = intent ?: return START_NOT_STICKY
        val url = safe.getStringExtra(Downloads.EXTRA_URL) ?: return START_NOT_STICKY
        val height = safe.getIntExtra(Downloads.EXTRA_HEIGHT, -1)
        val label = safe.getStringExtra(Downloads.EXTRA_LABEL) ?: "video"
        val start = safe.getIntExtra(Downloads.EXTRA_START, 0)
        val end = safe.getIntExtra(Downloads.EXTRA_END, 0)
        val title = safe.getStringExtra(Downloads.EXTRA_TITLE) ?: "video"
        val processId = safe.getStringExtra(Downloads.EXTRA_PROCESS_ID) ?: YtDlpRepo.newProcessId()

        runningProcessId = processId
        startForegroundCompat(title)

        job?.cancel()
        job = scope.launch {
            runDownload(
                url = url,
                quality = Quality(label, if (height < 0) null else height),
                start = start,
                end = end,
                title = title,
                processId = processId
            )
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        YtDlpRepo.cancel(runningProcessId)
        runningProcessId = null
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runDownload(
        url: String,
        quality: Quality,
        start: Int,
        end: Int,
        title: String,
        processId: String
    ) {
        val audio = quality.isAudio
        val ext = if (audio) "m4a" else "mp4"
        val outDir = File(cacheDir, "out")
        outDir.deleteRecursively()
        outDir.mkdirs()
        val outFile = File(outDir, "download.$ext")

        Downloads.update(DownloadState.Running(title, 0f, "Menyiapkan…"))
        val request = DownloadRequest(url, quality, start, end)
        val args = DownloadArgs.build(request, outFile.absolutePath, CookieStore.path(this))

        var lastLine = ""
        try {
            val exit = YtDlpRepo.execute(this, url, args, processId) { line ->
                lastLine = line
                parsePercent(line)?.let { pct ->
                    Downloads.update(DownloadState.Running(title, pct, line.trim()))
                    notifyProgress(title, pct, line.trim())
                }
            }
            if (Downloads.cancelRequested) {
                finish(DownloadState.Canceled, "Dibatalkan")
                return
            }
            if (exit != 0 || !outFile.exists() || outFile.length() == 0L) {
                val mapped = ErrorMapper.map(lastLine)
                finish(DownloadState.Failed(mapped.message, mapped.action), "Gagal: ${mapped.message}")
                return
            }
            val displayName = FileNames.displayName(title, start, end, ext)
            val uri = MediaStorePublisher.publish(this, outFile, displayName, audio)
            if (uri == null) {
                finish(DownloadState.Failed("Gagal menyimpan ke galeri"), "Gagal menyimpan ke galeri")
            } else {
                finish(DownloadState.Done(title, uri), "Selesai: ${FileNames.sanitize(title)}")
            }
        } catch (e: Exception) {
            if (Downloads.cancelRequested) {
                finish(DownloadState.Canceled, "Dibatalkan")
            } else {
                val mapped = ErrorMapper.map(e.message ?: "")
                finish(DownloadState.Failed(mapped.message, mapped.action), "Gagal: ${mapped.message}")
            }
        } finally {
            outDir.deleteRecursively()
            Downloads.finished()
        }
    }

    private fun finish(state: DownloadState, text: String) {
        Downloads.update(state)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        getSystemService(NotificationManager::class.java).notify(
            DONE_NOTIFICATION_ID,
            buildNotification(
                title = "YT Cutter",
                text = text,
                progress = 0f,
                indeterminate = false,
                ongoing = false
            )
        )
    }

    private fun parsePercent(line: String): Float? {
        if (!line.contains("[download]")) return null
        val match = Regex("""(\d{1,3}(?:\.\d+)?)%""").find(line) ?: return null
        return match.groupValues[1].toFloatOrNull()?.div(100f)
    }

    private fun startForegroundCompat(title: String) {
        val notification = buildNotification(
            title = title,
            text = "Menyiapkan…",
            progress = 0f,
            indeterminate = true,
            ongoing = true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                PROGRESS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(PROGRESS_NOTIFICATION_ID, notification)
        }
    }

    private fun notifyProgress(title: String, progress: Float, text: String) {
        getSystemService(NotificationManager::class.java).notify(
            PROGRESS_NOTIFICATION_ID,
            buildNotification(
                title = title,
                text = text,
                progress = progress,
                indeterminate = false,
                ongoing = true
            )
        )
    }

    private fun buildNotification(
        title: String,
        text: String,
        progress: Float,
        indeterminate: Boolean,
        ongoing: Boolean
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, (progress * 100).toInt(), indeterminate)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val PROGRESS_NOTIFICATION_ID = 1
        private const val DONE_NOTIFICATION_ID = 2
    }
}