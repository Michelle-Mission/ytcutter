package app.ytcutter.core.data

import app.ytcutter.core.model.Quality
import app.ytcutter.core.data.DownloadArgs
import app.ytcutter.core.model.DownloadRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadArgsTest {

    private val out = "/data/user/0/app.ytcutter/cache/out.mp4"

    @Test
    fun videoArgsCapHeightAndCutSection() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("1080p", 1080),
            start = 15,
            end = 220
        )
        assertEquals(
            listOf(
                "-f" to "bv*[height<=1080]+ba/b[height<=1080]/b",
                "-S" to "ext:mp4:m4a,res:1080",
                "--merge-output-format" to "mp4",
                "--concurrent-fragments" to "8",
                "--retries" to "2",
                "--fragment-retries" to "2",
                "--socket-timeout" to "20",
                "--no-mtime" to null,
                "--download-sections" to "*00:00:15-00:03:40",
                "--no-playlist" to null,
                "--newline" to null,
                "--continue" to null,
                "--progress-template" to "download:YTC %(progress.downloaded_bytes)s %(progress.total_bytes_estimate)s %(progress._percent_str)s %(progress._speed_str)s %(progress._eta_str)s",
                "-o" to out
            ),
            DownloadArgs.build(req, out, cookiesPath = null)
        )
    }

    @Test
    fun audioArgsExtractM4a() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("Audio", null),
            start = 0,
            end = 65
        )
        assertEquals(
            listOf(
                "-f" to "ba/b",
                "-x" to null,
                "--audio-format" to "m4a",
                "--concurrent-fragments" to "8",
                "--retries" to "2",
                "--fragment-retries" to "2",
                "--socket-timeout" to "20",
                "--no-mtime" to null,
                "--download-sections" to "*00:00:00-00:01:05",
                "--no-playlist" to null,
                "--newline" to null,
                "--continue" to null,
                "--progress-template" to "download:YTC %(progress.downloaded_bytes)s %(progress.total_bytes_estimate)s %(progress._percent_str)s %(progress._speed_str)s %(progress._eta_str)s",
                "-o" to out
            ),
            DownloadArgs.build(req, out, cookiesPath = null)
        )
    }

    @Test
    fun cookiesPathIsPassed() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("720p", 720),
            start = 0,
            end = 10
        )
        val args = DownloadArgs.build(req, out, cookiesPath = "/files/cookies.txt")
        assertEquals("--cookies" to "/files/cookies.txt", args.first())
    }

    @Test
    fun resumeAndProgressTemplateAlwaysPresent() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("720p", 720),
            start = 0,
            end = 10
        )
        val args = DownloadArgs.build(req, out, cookiesPath = null)
        assertTrue(args.any { it.first == "--continue" })
        assertTrue(args.any { it.first == "--progress-template" && it.second!!.startsWith("download:YTC ") })
    }

    @Test
    fun sectionTimePadsHours() {
        assertEquals("01:02:03", DownloadArgs.sectionTime(3723))
        assertEquals("00:00:05", DownloadArgs.sectionTime(5))
    }
}