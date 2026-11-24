package app.ytcutter.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun forbiddenMatchesFormatRejections() {
        assertTrue(RetryPolicy.isForbidden("ERROR: unable to download video data: HTTP Error 403: Forbidden"))
        assertTrue(RetryPolicy.isForbidden("ERROR: requested format is not available"))
        assertFalse(RetryPolicy.isForbidden("ERROR: Video unavailable"))
    }

    @Test
    fun transientStillHandlesNetwork() {
        assertTrue(RetryPolicy.isTransient("Read timed out"))
        assertTrue(RetryPolicy.isTransient("HTTP Error 503: Service Unavailable"))
        assertFalse(RetryPolicy.isTransient(""))
    }
}
