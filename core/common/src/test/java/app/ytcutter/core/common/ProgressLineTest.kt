package app.ytcutter.core.common

import app.ytcutter.core.common.ProgressLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressLineTest {

    @Test
    fun parsesByteBasedTemplateLine() {
        val p = ProgressLine.parse("YTC 5242880 10485760 50.0% 1.20MiB/s 00:04")!!
        assertEquals(50, p.percent)
        assertEquals("1.20MiB/s · ETA 00:04", p.detail)
    }

    @Test
    fun byteRatioWinsOverStalePercent() {
        assertEquals(75, ProgressLine.parse("YTC 7864320 10485760 12.0% 900KiB/s 00:02")!!.percent)
    }

    @Test
    fun fallsBackToPercentWhenTotalUnknown() {
        assertEquals(42, ProgressLine.parse("YTC NA NA 42.0% NA NA")!!.percent)
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