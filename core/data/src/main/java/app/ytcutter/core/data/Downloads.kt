package app.ytcutter.core.data

import app.ytcutter.core.model.DownloadRequest
import app.ytcutter.core.model.DownloadStatus

import android.content.Context
import app.ytcutter.core.database.HistoryRepo
import app.ytcutter.core.data.work.ActiveWork
import app.ytcutter.core.data.work.CancelRules
import app.ytcutter.core.data.work.QueueWorker

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
        if (CancelRules.shouldKill(id, ActiveWork.runningId)) {
            ActiveWork.cancelId = id
            YtDlpRepo.cancel(ActiveWork.processId)
        }
        HistoryRepo.get(context).cancel(id)
    }

    fun retry(context: Context, id: Long) {
        if (ActiveWork.cancelId == id) ActiveWork.cancelId = null
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