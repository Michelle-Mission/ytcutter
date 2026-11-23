package app.ytcutter.data

import android.content.Context
import app.ytcutter.data.history.DownloadStatus
import app.ytcutter.data.history.HistoryRepo
import app.ytcutter.work.ActiveWork
import app.ytcutter.work.QueueWorker

/** Fasad kecil antara UI dan antrean WorkManager + DB riwayat. */
object Downloads {

    fun enqueue(context: Context, request: DownloadRequest, title: String): Boolean {
        val repo = HistoryRepo.get(context)
        if (repo.downloads.value.any { it.status == DownloadStatus.RUNNING }) return false
        repo.enqueue(request, title)
        QueueWorker.ensureRunning(context)
        return true
    }

    fun cancel(context: Context, id: Long) {
        ActiveWork.cancelId = id
        YtDlpRepo.cancel(ActiveWork.processId)
        HistoryRepo.get(context).cancel(id)
    }

    fun retry(context: Context, id: Long) {
        HistoryRepo.get(context).retry(id)
        QueueWorker.ensureRunning(context)
    }

    fun resumePending(context: Context) {
        val repo = HistoryRepo.get(context)
        if (repo.downloads.value.any {
                it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING
            }
        ) {
            QueueWorker.ensureRunning(context)
        }
    }
}