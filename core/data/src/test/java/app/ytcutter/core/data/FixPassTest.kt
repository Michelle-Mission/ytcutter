package app.ytcutter.core.data

import app.ytcutter.core.data.DownloadArgs
import app.ytcutter.core.data.InfoCache
import app.ytcutter.core.common.NetscapeCookies
import app.ytcutter.core.data.NewPipeMapper
import app.ytcutter.core.data.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FixPassTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun cookieHeaderKeepsHttpOnlyLines() {
        val lines = listOf(
            "# Netscape HTTP Cookie File",
            "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t1999999999\tSID\tabc",
            ".youtube.com\tTRUE\t/\tFALSE\t1999999999\tPREF\tid=1"
        )
        assertEquals("SID=abc; PREF=id=1", NetscapeCookies.header(lines))
        assertNull(NetscapeCookies.header(listOf("# komentar saja")))
    }

    @Test
    fun existingPathFindsFileFromDisk() {
        val dir = tmp.newFolder("cache")
        val file = java.io.File(dir, "info-dQw4w9WgXcQ.json").apply { writeText("{}") }
        assertEquals(
            file.absolutePath,
            InfoCache.existingPath(dir, "https://youtu.be/dQw4w9WgXcQ")
        )
        assertNull(InfoCache.existingPath(dir, "https://youtu.be/aaaaaaaaaaa"))
    }

    @Test
    fun usableOnlyWhenThereAreStreams() {
        val empty = VideoInfo("id", "t", 10, null, emptySet(), false)
        val audio = VideoInfo("id", "t", 10, null, emptySet(), true)
        assertEquals(false, NewPipeMapper.isUsable(empty))
        assertEquals(true, NewPipeMapper.isUsable(audio))
    }

    @Test
    fun stripsInfoJsonOption() {
        val options = listOf(
            "--load-info-json" to "/cache/info-x.json",
            "-f" to "bv*",
            "--newline" to null
        )
        assertEquals(
            listOf("-f" to "bv*", "--newline" to null),
            DownloadArgs.stripInfoJson(options)
        )
    }
}
