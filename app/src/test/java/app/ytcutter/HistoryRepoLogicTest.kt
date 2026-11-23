package app.ytcutter

import app.ytcutter.data.history.DownloadStatus
import app.ytcutter.data.history.HistoryRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRepoLogicTest {

    @Test
    fun canRetryOnlyUnfinishedFailures() {
        assertTrue(HistoryRules.canRetry(DownloadStatus.FAILED))
        assertTrue(HistoryRules.canRetry(DownloadStatus.INTERRUPTED))
        assertTrue(HistoryRules.canRetry(DownloadStatus.CANCELED))
        assertFalse(HistoryRules.canRetry(DownloadStatus.QUEUED))
        assertFalse(HistoryRules.canRetry(DownloadStatus.RUNNING))
        assertFalse(HistoryRules.canRetry(DownloadStatus.DONE))
    }

    @Test
    fun finalStatuses() {
        assertTrue(HistoryRules.isFinal(DownloadStatus.DONE))
        assertTrue(HistoryRules.isFinal(DownloadStatus.CANCELED))
        assertFalse(HistoryRules.isFinal(DownloadStatus.QUEUED))
        assertFalse(HistoryRules.isFinal(DownloadStatus.RUNNING))
    }
}