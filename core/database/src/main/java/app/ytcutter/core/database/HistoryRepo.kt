package app.ytcutter.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import app.ytcutter.core.model.Action
import app.ytcutter.core.model.DownloadRequest
import app.ytcutter.core.model.DownloadRow
import app.ytcutter.core.model.DownloadStatus
import app.ytcutter.core.common.UrlValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HistoryRules {
    fun canRetry(status: DownloadStatus): Boolean = when (status) {
        DownloadStatus.FAILED, DownloadStatus.INTERRUPTED, DownloadStatus.CANCELED -> true
        else -> false
    }

    fun isFinal(status: DownloadStatus): Boolean = when (status) {
        DownloadStatus.DONE, DownloadStatus.CANCELED -> true
        else -> false
    }
}

class HistoryRepo private constructor(context: Context) {

    private val db = HistoryDb(context)

    private val _downloads = MutableStateFlow<List<DownloadRow>>(emptyList())
    val downloads: StateFlow<List<DownloadRow>> = _downloads.asStateFlow()

    init {
        refresh()
    }

    fun enqueue(request: DownloadRequest, title: String): Long {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("video_id", UrlValidator.videoId(request.url))
            put("url", request.url)
            put("title", title)
            put("quality_label", request.quality.label)
            put("height", request.quality.height ?: 0)
            put("start_sec", request.start)
            put("end_sec", request.end)
            put("status", DownloadStatus.QUEUED.dbValue)
            put("progress", 0)
            put("error_action", Action.NONE.name)
            put("created_at", now)
            put("updated_at", now)
        }
        val id = db.writableDatabase.insert("downloads", null, values)
        refresh()
        return id
    }

    fun markRunning(id: Long) = write(id) {
        put("status", DownloadStatus.RUNNING.dbValue)
        put("progress", 0)
        putNull("detail")
        putNull("error")
        put("error_action", Action.NONE.name)
    }

    fun updateProgress(id: Long, percent: Int, detail: String?) = write(id) {
        put("status", DownloadStatus.RUNNING.dbValue)
        put("progress", percent.coerceIn(0, 100))
        put("detail", detail)
    }

    fun finish(
        id: Long,
        status: DownloadStatus,
        uri: String? = null,
        fileName: String? = null,
        error: String? = null,
        action: Action = Action.NONE
    ) = write(id) {
        put("status", status.dbValue)
        put("error", error)
        put("error_action", action.name)
        uri?.let { put("uri", it) }
        fileName?.let { put("file_name", it) }
        if (status == DownloadStatus.DONE) put("progress", 100)
    }

    fun cancel(id: Long) {
        val row = get(id) ?: return
        if (row.status != DownloadStatus.DONE) finish(id, DownloadStatus.CANCELED)
    }

    fun retry(id: Long) {
        if (get(id)?.let { HistoryRules.canRetry(it.status) } != true) return
        write(id) {
            put("status", DownloadStatus.QUEUED.dbValue)
            put("progress", 0)
            putNull("detail")
            putNull("error")
            put("error_action", Action.NONE.name)
            putNull("uri")
            putNull("file_name")
        }
    }

    fun delete(id: Long) {
        db.writableDatabase.delete("downloads", "id = ?", arrayOf(id.toString()))
        refresh()
    }

    fun nextQueued(): DownloadRow? = query(
        selection = "status = ?",
        args = arrayOf(DownloadStatus.QUEUED.dbValue),
        order = "created_at ASC, id ASC",
        limit = 1
    ).firstOrNull()

    fun sweepInterrupted(): Int {
        val values = ContentValues().apply {
            put("status", DownloadStatus.QUEUED.dbValue)
            put("progress", 0)
            putNull("detail")
            put("updated_at", System.currentTimeMillis())
        }
        val n = db.writableDatabase.update(
            "downloads",
            values,
            "status = ?",
            arrayOf(DownloadStatus.RUNNING.dbValue)
        )
        if (n > 0) refresh()
        return n
    }

    fun get(id: Long): DownloadRow? = query("id = ?", arrayOf(id.toString()), null, 1).firstOrNull()

    internal fun refresh() {
        _downloads.value = query(null, null, "created_at DESC, id DESC", 200)
    }

    private fun write(id: Long, block: ContentValues.() -> Unit) {
        val values = ContentValues().apply {
            block()
            put("updated_at", System.currentTimeMillis())
        }
        db.writableDatabase.update("downloads", values, "id = ?", arrayOf(id.toString()))
        refresh()
    }

    private fun query(
        selection: String?,
        args: Array<String>?,
        order: String?,
        limit: Int
    ): List<DownloadRow> {
        val rows = mutableListOf<DownloadRow>()
        db.readableDatabase.query(
            "downloads", null, selection, args, null, null, order, limit.toString()
        ).use { c ->
            while (c.moveToNext()) rows += c.toRow()
        }
        return rows
    }

    private fun Cursor.toRow(): DownloadRow = DownloadRow(
        id = getLong(getColumnIndexOrThrow("id")),
        videoId = getStringOrNull("video_id"),
        url = getString(getColumnIndexOrThrow("url")),
        title = getString(getColumnIndexOrThrow("title")),
        qualityLabel = getString(getColumnIndexOrThrow("quality_label")),
        height = getInt(getColumnIndexOrThrow("height")).takeIf { it > 0 },
        startSec = getInt(getColumnIndexOrThrow("start_sec")),
        endSec = getInt(getColumnIndexOrThrow("end_sec")),
        status = DownloadStatus.fromDb(getString(getColumnIndexOrThrow("status"))),
        progress = getInt(getColumnIndexOrThrow("progress")),
        detail = getStringOrNull("detail"),
        error = getStringOrNull("error"),
        errorAction = runCatching {
            Action.valueOf(getString(getColumnIndexOrThrow("error_action")))
        }.getOrDefault(Action.NONE),
        uri = getStringOrNull("uri"),
        fileName = getStringOrNull("file_name"),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at"))
    )

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    companion object {
        @Volatile
        private var instance: HistoryRepo? = null

        fun get(context: Context): HistoryRepo = instance ?: synchronized(this) {
            instance ?: HistoryRepo(context.applicationContext).also { instance = it }
        }
    }
}