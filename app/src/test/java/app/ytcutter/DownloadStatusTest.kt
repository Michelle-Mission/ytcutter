package app.ytcutter

import app.ytcutter.data.history.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStatusTest {

    @Test
    fun roundTripsEveryStatus() {
        DownloadStatus.entries.forEach {
            assertEquals(it, DownloadStatus.fromDb(it.dbValue))
        }
    }

    @Test
    fun unknownValueBecomesFailed() {
        assertEquals(DownloadStatus.FAILED, DownloadStatus.fromDb("WHATEVER"))
    }
}