package app.ytcutter.core.data

object NewPipeMapper {

    private const val MIN_VIDEO_HEIGHT = 144

    fun isUsable(info: VideoInfo): Boolean = info.heights.isNotEmpty() || info.hasAudio

    fun toVideoInfo(
        videoId: String,
        title: String,
        durationSec: Int,
        thumbnail: String?,
        heights: Collection<Int>,
        hasAudio: Boolean
    ): VideoInfo = VideoInfo(
        id = videoId,
        title = title,
        durationSec = durationSec,
        thumbnailUrl = thumbnail?.takeIf { it.isNotBlank() },
        heights = heights.filter { it >= MIN_VIDEO_HEIGHT }.toSortedSet(compareByDescending { it }),
        hasAudio = hasAudio
    )
}
