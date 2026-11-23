package app.ytcutter.core.data

data class VideoInfo(
    val id: String,
    val title: String,
    val durationSec: Int,
    val thumbnailUrl: String?,
    val heights: Set<Int>,
    val hasAudio: Boolean
)