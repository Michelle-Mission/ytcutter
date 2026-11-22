package app.ytcutter

import app.ytcutter.core.TimeCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeCodeTest {

    @Test
    fun parsesMmSs() {
        assertEquals(83, TimeCode.parse("01:23"))
    }

    @Test
    fun parsesHhMmSs() {
        assertEquals(3723, TimeCode.parse("1:02:03"))
    }

    @Test
    fun parsesBareSeconds() {
        assertEquals(83, TimeCode.parse("83"))
    }

    @Test
    fun rejectsEmpty() {
        assertNull(TimeCode.parse(""))
        assertNull(TimeCode.parse("   "))
    }

    @Test
    fun rejectsOutOfRangeMinutesAndSeconds() {
        assertNull(TimeCode.parse("01:75"))
        assertNull(TimeCode.parse("61:00"))
    }

    @Test
    fun rejectsNegative() {
        assertNull(TimeCode.parse("-1"))
    }

    @Test
    fun rejectsNonNumeric() {
        assertNull(TimeCode.parse("ab:cd"))
        assertNull(TimeCode.parse("1:2:3:4"))
    }

    @Test
    fun formatsShort() {
        assertEquals("01:23", TimeCode.format(83))
        assertEquals("00:00", TimeCode.format(0))
    }

    @Test
    fun formatsLong() {
        assertEquals("1:02:03", TimeCode.format(3723))
    }

    @Test
    fun roundTrip() {
        listOf(0, 59, 60, 83, 599, 3600, 3723).forEach {
            assertEquals(it, TimeCode.parse(TimeCode.format(it)))
        }
    }
}