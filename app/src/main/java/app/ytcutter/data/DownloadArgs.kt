package app.ytcutter.data

import app.ytcutter.core.FormatMapper
import app.ytcutter.core.Quality

data class DownloadRequest(
    val url: String,
    val quality: Quality,
    val start: Int,
    val end: Int
)

object DownloadArgs {

    fun build(
        req: DownloadRequest,
        outPath: String,
        cookiesPath: String?
    ): List<Pair<String, String?>> {
        val args = mutableListOf<Pair<String, String?>>()
        cookiesPath?.let { args += "--cookies" to it }

        if (req.quality.isAudio) {
            args += "-f" to FormatMapper.audioSelector()
            args += "-x" to null
            args += "--audio-format" to "m4a"
        } else {
            val height = req.quality.height ?: 0
            args += "-f" to FormatMapper.videoSelector(height)
            args += "-S" to "ext:mp4:m4a,res:$height"
            args += "--merge-output-format" to "mp4"
        }

        args += "--download-sections" to "*${sectionTime(req.start)}-${sectionTime(req.end)}"
        if (!req.quality.isAudio) args += "--force-keyframes-at-cuts" to null
        args += "--no-playlist" to null
        args += "--newline" to null
        args += "-o" to outPath
        return args
    }

    fun sectionTime(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
}