package app.ytcutter.core.model

import app.ytcutter.core.model.DownloadStatus
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