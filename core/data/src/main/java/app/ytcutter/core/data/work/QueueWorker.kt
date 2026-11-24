package app.ytcutter.core.data.work

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.ytcutter.core.model.Quality
import app.ytcutter.core.model.Action
import app.ytcutter.core.data.Aria2
import app.ytcutter.core.data.CookieStore
import app.ytcutter.core.data.DownloadArgs
import app.ytcutter.core.model.DownloadRequest
import app.ytcutter.core.data.ErrorMapper
import app.ytcutter.core.common.FileNames
import app.ytcutter.core.data.InfoCache
import app.ytcutter.core.data.MediaStorePublisher
import app.ytcutter.core.common.ProgressLine
import app.ytcutter.core.common.RetryPolicy
import app.ytcutter.core.common.DeviceProfile
import app.ytcutter.core.common.StageLine
import app.ytcutter.core.data.YtDlpRepo
import app.ytcutter.core.data.cookies.CookiePrefs
import app.ytcutter.core.model.DownloadRow
import app.ytcutter.core.model.DownloadStatus
import app.ytcutter.core.database.HistoryRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File

class QueueWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = HistoryRepo.get(applicationContext)
        repo.sweepInterrupted()
        while (!isStopped) {
            val row = repo.nextQueued() ?: break
            runItem(repo, row)
        }
        return Result.success()
    }

    private suspend fun runItem(repo: HistoryRepo, row: DownloadRow) {
        val audio = row.height == null
        val ext = if (audio) "m4a" else "mp4"
        val outDir = File(applicationContext.cacheDir, "dl/${row.id}")
        outDir.mkdirs()
        val outFile = File(outDir, "download.$ext")
        val cookies = if (CookiePrefs.useCookies(applicationContext)) {
            CookieStore.path(applicationContext)
        } else {
            null
        }
        val infoJson = InfoCache.existingPath(applicationContext.cacheDir, row.url)

        var attempt = 0
        var usedFallback = false
        var ariaEnabled = Aria2.available(applicationContext)
        while (true) {
            if (ActiveWork.cancelId == row.id) {
                ActiveWork.cancelId = null
                outDir.deleteRecursively()
                repo.finish(row.id, DownloadStatus.CANCELED)
                return
            }
            repo.markRunning(row.id)
            val processId = YtDlpRepo.newProcessId()
            ActiveWork.processId = processId
            ActiveWork.runningId = row.id
            val request = DownloadRequest(
                url = row.url,
                quality = Quality(row.qualityLabel, row.height),
                start = row.startSec,
                end = row.endSec,
                infoJson = infoJson
            )
            val profile = DeviceProfile.of(applicationContext)
            val args = DownloadArgs.build(
                request,
                outFile.absolutePath,
                cookies,
                profile.fragments,
                fallback = usedFallback,
                aria2 = if (ariaEnabled) Aria2.args(applicationContext, profile.fragments) else emptyList()
            )

            var lastLine = ""
            var latestPercent = 0
            var latestDetail: String? = null
            var canceled = false
            var publishFailed = false
            val notifierScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            val notifier = notifierScope.launch {
                while (true) {
                    runCatching {
                        setForeground(
                            DownloadNotifications.progress(
                                applicationContext,
                                row.title,
                                latestPercent.coerceAtLeast(0),
                                latestDetail,
                                indeterminate = latestPercent < 0
                            )
                        )
                    }
                    delay(500)
                }
            }
            val exit = try {
                YtDlpRepo.execute(applicationContext, row.url, args, processId) { line ->
                    lastLine = line
                    val stage = StageLine.parse(line)
                    if (stage != null) {
                        latestPercent = -1
                        latestDetail = stage
                        runCatching { repo.updateProgress(row.id, -1, stage) }
                    } else {
                        ProgressLine.parse(line)?.let { p ->
                            latestPercent = p.percent
                            latestDetail = p.detail
                            runCatching { repo.updateProgress(row.id, p.percent, p.detail) }
                        }
                    }
                }
            } catch (canceled: kotlinx.coroutines.CancellationException) {
                notifier.cancel()
                notifierScope.cancel()
                ActiveWork.processId = null
                ActiveWork.runningId = null
                throw canceled
            } catch (error: Exception) {
                lastLine = error.message ?: ""
                -1
            }
            notifier.cancel()
            notifierScope.cancel()
            ActiveWork.processId = null
            ActiveWork.runningId = null

            if (ActiveWork.cancelId == row.id) {
                ActiveWork.cancelId = null
                canceled = true
            }

            if (!canceled && exit == 0 && outFile.exists() && outFile.length() > 0L) {
                latestPercent = -1
                latestDetail = "Menyimpan ke galeri"
                runCatching { repo.updateProgress(row.id, -1, latestDetail) }
                val displayName = FileNames.displayName(row.title, row.startSec, row.endSec, ext)
                val uri = MediaStorePublisher.publish(applicationContext, outFile, displayName, audio)
                if (uri != null) {
                    repo.finish(row.id, DownloadStatus.DONE, uri.toString(), displayName)
                    outDir.deleteRecursively()
                    DownloadNotifications.finished(
                        applicationContext,
                        row.title,
                        "Selesai: ${FileNames.sanitize(row.title)}"
                    )
                    return
                }
                lastLine = "Gagal menyimpan ke galeri"
                publishFailed = true
            }

            if (canceled) {
                outDir.deleteRecursively()
                repo.finish(row.id, DownloadStatus.CANCELED)
                return
            }

            val mapped = ErrorMapper.map(lastLine.ifBlank { "yt-dlp keluar dengan kode $exit" })
            if (ariaEnabled && exit != 0) {
                ariaEnabled = false
                outFile.delete()
                java.io.File("${outFile.absolutePath}.part").delete()
                java.io.File("${outFile.absolutePath}.aria2").delete()
                continue
            }
            if (!usedFallback && RetryPolicy.isForbidden(lastLine)) {
                usedFallback = true
                outFile.delete()
                java.io.File("${outFile.absolutePath}.part").delete()
                continue
            }
            if (RetryPolicy.isTransient(lastLine) && attempt < 2) {
                attempt++
                delay(5_000)
                continue
            }

            // File hasil tetap disimpan bila hanya publish ke galeri yang gagal,
            // supaya percobaan ulang tidak mengunduh ulang seluruh video.
            if (!publishFailed) outDir.deleteRecursively()
            repo.finish(row.id, DownloadStatus.FAILED, error = mapped.message, action = mapped.action)
            DownloadNotifications.finished(
                applicationContext,
                row.title,
                "Gagal: ${mapped.message}"
            )
            return
        }
    }

    companion object {
        const val NAME = "download-queue"
        private const val KEY_PERCENT = "percent"
        private const val KEY_DETAIL = "detail"

        fun ensureRunning(context: Context) {
            val request = OneTimeWorkRequestBuilder<QueueWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}