package app.ytcutter.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StageLineTest {

    @Test
    fun mapsYtDlpStages() {
        assertEquals(
            "Menggabungkan video dan audio",
            StageLine.parse("[Merger] Merging formats into \"x.mp4\"")
        )
        assertEquals("Mengekstrak audio", StageLine.parse("[ExtractAudio] Destination: a.m4a"))
        assertEquals("Mengambil tautan video", StageLine.parse("[youtube] Extracting URL: https://youtu.be/x"))
        assertEquals("Mengunduh berkas", StageLine.parse("[download] Destination: y.mp4"))
    }

    @Test
    fun ignoresUnknownLines() {
        assertNull(StageLine.parse("YTC 1024 2048  50.0% 1MiB/s 00:02"))
        assertNull(StageLine.parse(""))
    }
}
