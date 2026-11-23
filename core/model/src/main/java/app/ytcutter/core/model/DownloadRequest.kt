package app.ytcutter.core.model

data class DownloadRequest(
    val url: String,
    val quality: Quality,
    val start: Int,
    val end: Int,
    val infoJson: String? = null
)
