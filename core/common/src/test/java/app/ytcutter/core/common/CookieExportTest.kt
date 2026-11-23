package app.ytcutter.core.common

import app.ytcutter.core.common.CookieExport
import app.ytcutter.core.common.CookieRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieExportTest {

    @Test
    fun parsesCookieHeader() {
        val records = CookieExport.fromCookieHeader(
            "SID=abc; HSID=def; PREF=tz=Asia.Jakarta",
            ".youtube.com",
            expires = 1000L
        )
        assertEquals(3, records.size)
        assertEquals("SID", records[0].name)
        assertEquals("abc", records[0].value)
        assertEquals("tz=Asia.Jakarta", records[2].value)
        assertEquals(".youtube.com", records[0].domain)
        assertEquals(1000L, records[0].expires)
        assertTrue(records[0].secure)
    }

    @Test
    fun ignoresEmptyAndBrokenPairs() {
        val records = CookieExport.fromCookieHeader("; ; =x; GOOD=1;", ".youtube.com", 5L)
        assertEquals(1, records.size)
        assertEquals("GOOD", records[0].name)
    }

    @Test
    fun netscapeFormatHasHeaderAndTabs() {
        val text = CookieExport.toNetscape(
            listOf(
                CookieRecord(".youtube.com", "SID", "abc", 1999999999L, secure = true),
                CookieRecord(".google.com", "NID", "xyz", 1999999999L, secure = false)
            )
        )
        val lines = text.trim().lines()
        assertEquals("# Netscape HTTP Cookie File", lines.first())
        assertEquals(".youtube.com\tTRUE\t/\tTRUE\t1999999999\tSID\tabc", lines[1])
        assertEquals(".google.com\tTRUE\t/\tFALSE\t1999999999\tNID\txyz", lines[2])
    }

    @Test
    fun deduplicatesSameDomainAndName() {
        val merged = CookieExport.merge(
            listOf(
                CookieRecord(".youtube.com", "SID", "old", 1L),
                CookieRecord(".youtube.com", "SID", "new", 2L)
            )
        )
        assertEquals(1, merged.size)
        assertEquals("new", merged.first().value)
    }
}