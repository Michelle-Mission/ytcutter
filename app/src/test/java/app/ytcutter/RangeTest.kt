package app.ytcutter

import app.ytcutter.core.Range
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeTest {

    @Test
    fun keepsValidRange() {
        assertEquals(15 to 220, Range.clamp(15, 220, 761))
    }

    @Test
    fun clampsEndToDuration() {
        assertEquals(15 to 761, Range.clamp(15, 9999, 761))
    }

    @Test
    fun clampsNegativeStart() {
        assertEquals(0 to 60, Range.clamp(-5, 60, 761))
    }

    @Test
    fun enforcesMinimumLength() {
        val (start, end) = Range.clamp(100, 100, 761)
        assertEquals(100, start)
        assertEquals(102, end)
    }

    @Test
    fun tinyDurationsDoNotThrow() {
        assertEquals(0 to 0, Range.clamp(0, 0, 0))
        assertEquals(0 to 1, Range.clamp(0, 5, 1))
        assertEquals(0 to 2, Range.clamp(0, 5, 2))
        assertFalse(Range.isSelectable(2))
        assertTrue(Range.isSelectable(3))
    }

    @Test
    fun startTooCloseToEndPullsStartBack() {
        val (start, end) = Range.clamp(760, 761, 761)
        assertEquals(759, start)
        assertEquals(761, end)
    }
}