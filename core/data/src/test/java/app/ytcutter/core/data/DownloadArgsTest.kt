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
    fun videoArgsPreferCompatAndAreRobust() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("1080p", 1080),
            start = 15,
            end = 220
        )
        val args = DownloadArgs.build(req, out, cookiesPath = null)
        val map = args.toMap()
        assertEquals("bv*[height<=1080]+ba/b[height<=1080]/b", map["-f"])
        assertEquals("res:1080,ext:mp4:m4a,vcodec:h264,acodec:m4a", map["-S"])
        assertEquals("mp4", map["--merge-output-format"])
        assertEquals("10", map["--retries"])
        assertEquals("10", map["--fragment-retries"])
        assertEquals("3", map["--file-access-retries"])
        assertEquals("15", map["--socket-timeout"])
        assertEquals("10M", map["--http-chunk-size"])
        assertEquals("100K", map["--throttled-rate"])
        assertEquals("*00:00:15-00:03:40", map["--download-sections"])
        assertEquals(out, map["-o"])
        assertTrue(args.any { it.first == "--continue" })
    }

    @Test
    fun fallbackUsesProgressiveFormats() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("1080p", 1080),
            start = 0,
            end = 65
        )
        val map = DownloadArgs.build(req, out, cookiesPath = null, fallback = true).toMap()
        assertEquals("b[height<=1080][ext=mp4]/b[height<=1080]/b", map["-f"])
        assertTrue(map["-S"] == null)
        assertEquals("mp4", map["--merge-output-format"])
    }

    @Test
    fun aria2ArgsAreForwarded() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("720p", 720),
            start = 0,
            end = 10
        )
        val aria = listOf(
            "--downloader" to "/lib/libaria2c.so",
            "--external-downloader-args" to "/lib/libaria2c.so:-x 4 -s 4"
        )
        val args = DownloadArgs.build(req, out, cookiesPath = null, aria2 = aria)
        assertTrue(args.containsAll(aria))
    }

    @Test
    fun audioArgsExtractM4a() {
        val req = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            quality = Quality("Audio", null),
            start = 0,
            end = 65
        )
        val map = DownloadArgs.build(req, out, cookiesPath = null).toMap()
        assertEquals("ba/b", map["-f"])
        assertEquals("m4a", map["--audio-format"])
        assertTrue(map.containsKey("-x"))
        assertEquals("*00:00:00-00:01:05", map["--download-sections"])
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