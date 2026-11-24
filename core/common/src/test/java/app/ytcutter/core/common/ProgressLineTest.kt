package app.ytcutter.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressLineTest {

    @Test
    fun parsesByteBasedTemplateLine() {
        val p = ProgressLine.parse("YTC 5242880 10485760 NA NA NA 50.0% 1.20MiB/s 00:04")!!
        assertEquals(50, p.percent)
        assertEquals("1.20MiB/s · ETA 00:04", p.detail)
    }

    @Test
    fun usesFragmentCountWhenBytesUnknown() {
        val p = ProgressLine.parse("YTC NA NA NA 12 40  0.0% 1.1MiB/s 00:20")!!
        assertEquals(30, p.percent)
    }

    @Test
    fun fallsBackToPercentWhenTotalsUnknown() {
        assertEquals(42, ProgressLine.parse("YTC NA NA NA NA NA 42.0% NA NA")!!.percent)
    }

    @Test
    fun noNumbersMeansIndeterminate() {
        assertEquals(-1, ProgressLine.parse("YTC NA NA NA NA NA NA 700KiB/s 00:03")!!.percent)
    }

    @Test
    fun stripsAnsiColorCodes() {
        val line = "YTC 512 1024 NA NA NA \u001B[0;32m 50.0%\u001B[0m 1MiB/s 00:01"
        assertEquals(50, ProgressLine.parse(line)!!.percent)
    }

    @Test
    fun fallsBackToLegacyDownloadLine() {
        assertEquals(10, ProgressLine.parse("[download]  10.5% of 9.00MiB at 1MiB/s ETA 00:09")!!.percent)
    }

    @Test
    fun ignoresNonProgressLines() {
        assertNull(ProgressLine.parse("[youtube] Extracting URL"))
        assertNull(ProgressLine.parse(""))
        assertNull(ProgressLine.parse("YTC 1 2 3"))
    }
}
