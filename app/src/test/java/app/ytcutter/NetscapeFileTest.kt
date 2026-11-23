package app.ytcutter

import app.ytcutter.data.cookies.NetscapeFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetscapeFileTest {

    @Test
    fun dedupesLinesAndKeepsHttpOnly() {
        val a = "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tSID\tabc\n.youtube.com\tTRUE\t/\tTRUE\t0\tPREF\tx"
        val b = "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tSID\tabc"
        val out = NetscapeFile.build(listOf(a, b))
        assertTrue(out.startsWith(NetscapeFile.HEADER))
        assertEquals(1, out.lines().count { it.contains("\tSID\t") })
        assertTrue(out.lines().any { it.startsWith("#HttpOnly_") })
    }

    @Test
    fun keepsEmptyValueAsSevenFields() {
        val out = NetscapeFile.build(listOf(".youtube.com\tTRUE\t/\tTRUE\t0\tEMPTY\t"))
        assertEquals(7, out.lines().last { it.isNotBlank() }.split('\t').size)
    }

    @Test
    fun dropsCommentsButKeepsHttpOnly() {
        val out = NetscapeFile.build(listOf("# komentar", "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tA\tb"))
        assertEquals(1, out.lines().count { it.isNotBlank() && !it.startsWith("# Netscape") })
    }

    @Test
    fun clipboardRequiresCookieLines() {
        assertNull(NetscapeFile.parseClipboard("halo dunia"))
        assertNotNull(
            NetscapeFile.parseClipboard(
                "# Netscape HTTP Cookie File\n.youtube.com\tTRUE\t/\tTRUE\t0\tA\tb"
            )
        )
    }

    @Test
    fun clipboardStripsHeaderComment() {
        val body = NetscapeFile.parseClipboard(
            "# Netscape HTTP Cookie File\n# catatan\n.youtube.com\tTRUE\t/\tTRUE\t0\tA\tb"
        )!!
        assertEquals(".youtube.com\tTRUE\t/\tTRUE\t0\tA\tb", body)
    }
}