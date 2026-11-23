package app.ytcutter.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor

object YouTubeInfo {

    @Volatile
    private var initialized = false

    suspend fun fetch(context: Context, url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInit(context)
            val service = NewPipe.getServiceByUrl(url)
            val info = StreamInfo.getInfo(service, url)
            val heights = (info.videoStreams + info.videoOnlyStreams)
                .map { it.height }
                .filter { it > 0 }
            val thumbnail = info.thumbnails
                .filter { it.height != org.schabi.newpipe.extractor.Image.HEIGHT_UNKNOWN }
                .maxByOrNull { it.height }
                ?.url
                ?: info.thumbnails.firstOrNull()?.url
            NewPipeMapper.toVideoInfo(
                videoId = info.id,
                title = info.name,
                durationSec = info.duration.toInt(),
                thumbnail = thumbnail,
                heights = heights,
                hasAudio = info.audioStreams.isNotEmpty()
            )
        }
    }

    private fun ensureInit(context: Context) {
        if (initialized) return
        NewPipe.init(NewPipeDownloader(context))
        setNewPipePoTokenProvider(NewPipePoTokenProvider(context))
        initialized = true
    }
}
