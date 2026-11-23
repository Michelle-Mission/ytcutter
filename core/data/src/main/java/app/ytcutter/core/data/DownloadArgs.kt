package app.ytcutter.core.data

import app.ytcutter.core.model.DownloadRequest
import app.ytcutter.core.model.Quality

import app.ytcutter.core.common.FormatMapper

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

        args += "--concurrent-fragments" to "8"
        args += "--retries" to "2"
        args += "--fragment-retries" to "2"
        args += "--socket-timeout" to "20"
        args += "--no-mtime" to null
        args += "--download-sections" to "*${sectionTime(req.start)}-${sectionTime(req.end)}"
        req.infoJson?.let { args += "--load-info-json" to it }
        args += "--no-playlist" to null
        args += "--newline" to null
        args += "--continue" to null
        args += "--progress-template" to "download:YTC %(progress.downloaded_bytes)s " +
            "%(progress.total_bytes_estimate)s %(progress._percent_str)s " +
            "%(progress._speed_str)s %(progress._eta_str)s"
        args += "-o" to outPath
        return args
    }

    fun stripInfoJson(options: List<Pair<String, String?>>): List<Pair<String, String?>> =
        options.filterNot { it.first == "--load-info-json" }

    fun sectionTime(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
}