package app.ytcutter.core.data

import app.ytcutter.core.data.cookies.WebViewCookieReader
import app.ytcutter.core.data.cookies.WebViewCookieReader.RawCookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewCookieReaderTest {

    @Test
    fun writesSevenFieldNetscapeLine() {
        val line = WebViewCookieReader.toLines(
            listOf(RawCookie(".youtube.com", "PREF", "x", "/", true, 123L, false))
        ).single()
        assertEquals(".youtube.com\tTRUE\t/\tTRUE\t123\tPREF\tx", line)
    }

    @Test
    fun hostWithoutDotGetsLeadingDot() {
        val line = WebViewCookieReader.toLines(
            listOf(RawCookie("youtube.com", "PREF", "x", "/", false, 0L, false))
        ).single()
        assertTrue(line.startsWith(".youtube.com\tTRUE\t/\tFALSE\t"))
    }

    @Test
    fun httpOnlyGetsPrefix() {
        val line = WebViewCookieReader.toLines(
            listOf(RawCookie(".youtube.com", "SID", "v", "/", true, 0L, true))
        ).single()
        assertTrue(line.startsWith("#HttpOnly_.youtube.com\t"))
    }

    @Test
    fun emptyValueStillSevenFields() {
        val line = WebViewCookieReader.toLines(
            listOf(RawCookie(".youtube.com", "EMPTY", "", "/", true, 0L, false))
        ).single()
        assertEquals(7, line.split('\t').size)
    }
}