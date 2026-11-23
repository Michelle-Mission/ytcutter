package app.ytcutter

import app.ytcutter.data.cookies.CookieCheck
import app.ytcutter.data.cookies.CookieStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CookieCheckTest {

    @Test
    fun accountsHostMeansLoggedOut() = assertEquals(
        CookieStatus.NOT_LOGGED_IN,
        CookieCheck.classify("https://accounts.google.com/ServiceLogin", 200)
    )

    @Test
    fun youtubeHostMeansLoggedIn() = assertEquals(
        CookieStatus.LOGGED_IN,
        CookieCheck.classify("https://www.youtube.com/feed/subscriptions", 200)
    )

    @Test
    fun consentHostIsUnknown() = assertEquals(
        CookieStatus.UNKNOWN,
        CookieCheck.classify("https://consent.youtube.com/m", 200)
    )

    @Test
    fun serverErrorIsUnknown() = assertEquals(
        CookieStatus.UNKNOWN,
        CookieCheck.classify("https://www.youtube.com/feed/subscriptions", 503)
    )

    @Test
    fun missingUrlIsUnknown() = assertEquals(
        CookieStatus.UNKNOWN,
        CookieCheck.classify(null, 200)
    )
}