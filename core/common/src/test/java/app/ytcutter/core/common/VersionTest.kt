package app.ytcutter.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {

    @Test
    fun stripsPrefixAndParses() {
        assertEquals(Version(0, 4, 0), Version.parse("v0.4.0"))
        assertEquals(Version(1, 2, 3), Version.parse("1.2.3"))
    }

    @Test
    fun comparesNumerically() {
        assertTrue(Version.parse("0.10.0")!! > Version.parse("0.9.9")!!)
        assertTrue(Version.parse("v1.0.0")!! > Version.parse("0.99.99")!!)
        assertFalse(Version.parse("0.4.0")!! > Version.parse("0.4.0")!!)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(Version.parse("latest"))
        assertNull(Version.parse("v1.x"))
    }
}
