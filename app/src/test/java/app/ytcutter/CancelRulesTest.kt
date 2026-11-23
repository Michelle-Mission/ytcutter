package app.ytcutter

import app.ytcutter.work.CancelRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelRulesTest {

    @Test
    fun onlyKillsProcessOfRunningItem() {
        assertTrue(CancelRules.shouldKill(targetId = 4, runningId = 4))
        assertFalse(CancelRules.shouldKill(targetId = 4, runningId = 5))
        assertFalse(CancelRules.shouldKill(targetId = 4, runningId = null))
    }
}