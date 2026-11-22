package app.ytcutter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.ytcutter.MainActivity
import app.ytcutter.core.FormatMapper
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

class DownloadService : android.app.Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Unduhan", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intentSafe = intent ?: return START_NOT_STICKY
        val url = intentSafe.getStringExtra(Downloads.EXTRA_URL) ?: return START_NOT_STICKY
        val height = intentSafe.getIntExtra(Downloads.EXTRA_HEIGHT, -1)
        val label = intentSafe.getStringExtra(Downloads.EXTRA_LABEL) ?: "video"
        val start = intentSafe.getIntExtra(Downloads.EXTRA_START, 0)
        val end = intentSafe.getIntExtra(Downloads.EXTRA_END, 0)
        val title = intentSafe.getStringExtra(Downloads.EXTRA_TITLE) ?: "video"

        startForegroundCompat(title)

        job?.cancel()
        job = scope.launch {
            runDownload(url, Quality(label, if (height < 0) null else height), start, end, title)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runDownload(url: String, quality: Quality, start: Int, end: Int, title: String) {
        val audio = quality.isAudio
        val ext = if (audio) "m4a" else "mp4"
        val outDir = File(cacheDir, "out").apply { mkdirs() }
        val outFile = File(outDir, "download.$ext")
        outFile.delete()

        Downloads.update(DownloadState.Running(title, 0f, "Menyiapkan…"))
        val request = DownloadRequest(url, quality, start, end)
        val args = DownloadArgs.build(request, outFile.absolutePath, CookieStore.path(this))

        var lastLine = ""
        try {
            val exit = YtDlpRepo.execute(this, url, args) { line ->
                lastLine = line
                parsePercent(line)?.let { pct ->
                    Downloads.update(DownloadState.Running(title, pct, line.trim()))
                    notifyProgress(title, pct, line.trim())
                }
            }
            val produced = outFile.takeIf { it.exists() && it.length() > 0 }
                ?: outDir.listFiles()?.firstOrNull { it.length() > 0 }
            if (Downloads.cancelRequested) {
                produced?.delete()
                Downloads.update(DownloadState.Canceled)
                notifyFinal("Dibatalkan")
                return
            }
            if (exit != 0 || produced == null) {
                val mapped = ErrorMapper.map(lastLine)
                Downloads.update(DownloadState.Failed(mapped.message))
                notifyFinal("Gagal: ${mapped.message}")
                produced?.delete()
                return
            }
            val displayName = FileNames.displayName(title, start, end, ext)
            val uri = MediaStorePublisher.publish(this, produced, displayName, audio)
            produced.delete()
            if (uri == null) {
                Downloads.update(DownloadState.Failed("Gagal menyimpan ke galeri"))
                notifyFinal("Gagal menyimpan ke galeri")
            } else {
                Downloads.update(DownloadState.Done(title, uri))
                notifyFinal("Selesai: ${FileNames.sanitize(title)}")
            }
        } catch (e: Exception) {
            outFile.delete()
            if (Downloads.cancelRequested) {
                Downloads.update(DownloadState.Canceled)
                notifyFinal("Dibatalkan")
                return
            }
            val mapped = ErrorMapper.map(e.message ?: "")
            Downloads.update(DownloadState.Failed(mapped.message))
            notifyFinal("Gagal: ${mapped.message}")
        } finally {
            outFile.delete()
        }
    }

    private fun parsePercent(line: String): Float? {
        if (!line.contains("[download]")) return null
        val match = Regex("""(\d{1,3}(?:\.\d+)?)%""").find(line) ?: return null
        return match.groupValues[1].toFloatOrNull()?.div(100f)
    }

    private fun startForegroundCompat(title: String) {
        val notification = buildNotification(
            title = title,
            progress = 0f,
            text = "Menyiapkan…",
            indeterminate = true,
            ongoing = true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyProgress(title: String, progress: Float, text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = title,
                progress = progress,
                text = text,
                indeterminate = false,
                ongoing = true
            )
        )
    }

    private fun notifyFinal(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(
            NOTIFICATION_ID,
            buildNotification(title = "YT Cutter", progress = 0f, text = text, indeterminate = false, ongoing = false)
        )
    }

    private fun buildNotification(
        title: String,
        progress: Float,
        text: String,
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
        private const val NOTIFICATION_ID = 1
    }
}