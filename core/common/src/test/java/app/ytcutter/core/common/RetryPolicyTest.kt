package app.ytcutter.core.common

import app.ytcutter.core.common.RetryPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun retriesTransientNetworkFailures() {
        assertTrue(RetryPolicy.isTransient("ERROR: unable to download video data: timed out"))
        assertTrue(RetryPolicy.isTransient("Connection reset by peer"))
        assertTrue(RetryPolicy.isTransient("HTTP Error 503: Service Unavailable"))
        assertTrue(RetryPolicy.isTransient("temporary failure in name resolution"))
    }

    @Test
    fun neverRetriesDeterministicFailures() {
        assertFalse(RetryPolicy.isTransient("Sign in to confirm you're not a bot"))
        assertFalse(RetryPolicy.isTransient("Requested format is not available"))
        assertFalse(RetryPolicy.isTransient("This video is private"))
        assertFalse(RetryPolicy.isTransient(""))
    }
}
