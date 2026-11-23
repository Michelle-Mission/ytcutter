package app.ytcutter.core.network

import app.ytcutter.core.model.CookieStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CookieClassifyTest {

    @Test
    fun accountsHostMeansLoggedOut() = assertEquals(
        CookieStatus.NOT_LOGGED_IN,
        CookieProbeApi.classify("https://accounts.google.com/ServiceLogin", 200)
    )

    @Test
    fun youtubeHostMeansLoggedIn() = assertEquals(
        CookieStatus.LOGGED_IN,
        CookieProbeApi.classify("https://www.youtube.com/feed/subscriptions", 200)
    )

    @Test
    fun consentHostIsUnknown() = assertEquals(
        CookieStatus.UNKNOWN,
        CookieProbeApi.classify("https://consent.youtube.com/m", 200)
    )

    @Test
    fun errorCodeIsUnknown() = assertEquals(
        CookieStatus.UNKNOWN,
        CookieProbeApi.classify("https://www.youtube.com/feed/subscriptions", 503)
    )
}
