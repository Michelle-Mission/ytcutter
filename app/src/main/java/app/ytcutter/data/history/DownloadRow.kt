package app.ytcutter.data.history

import app.ytcutter.data.Action

enum class DownloadStatus(val dbValue: String) {
    QUEUED("QUEUED"),
    RUNNING("RUNNING"),
    DONE("DONE"),
    FAILED("FAILED"),
    CANCELED("CANCELED"),
    INTERRUPTED("INTERRUPTED");

    companion object {
        fun fromDb(text: String): DownloadStatus =
            entries.firstOrNull { it.dbValue == text } ?: FAILED
    }
}

data class DownloadRow(
    val id: Long,
    val videoId: String?,
    val url: String,
    val title: String,
    val qualityLabel: String,
    val height: Int?,
    val startSec: Int,
    val endSec: Int,
    val status: DownloadStatus,
    val progress: Int,
    val detail: String?,
    val error: String?,
    val errorAction: Action,
    val uri: String?,
    val fileName: String?,
    val createdAt: Long,
    val updatedAt: Long
)