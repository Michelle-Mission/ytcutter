package app.ytcutter.core.datastore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPrefsTest {

    @Test
    fun storedValueWins() {
        assertTrue(UserPrefs.migrate(old = false, stored = true))
        assertFalse(UserPrefs.migrate(old = true, stored = false))
    }

    @Test
    fun oldValueUsedWhenNothingStored() {
        assertTrue(UserPrefs.migrate(old = true, stored = null))
    }

    @Test
    fun defaultsToOff() {
        assertFalse(UserPrefs.migrate(old = null, stored = null))
    }
}
