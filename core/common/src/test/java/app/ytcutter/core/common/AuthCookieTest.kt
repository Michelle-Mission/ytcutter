package app.ytcutter.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCookieTest {

    @Test
    fun sidMeansLoggedIn() {
        assertTrue(AuthCookie.isLoggedIn(listOf(".youtube.com\tTRUE\t/\tTRUE\t0\tSID\tabc")))
    }

    @Test
    fun securePsidMeansLoggedIn() {
        assertTrue(AuthCookie.isLoggedIn(listOf("#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\t__Secure-1PSID\tx")))
    }

    @Test
    fun loginInfoMeansLoggedIn() {
        assertTrue(AuthCookie.isLoggedIn(listOf(".youtube.com\tTRUE\t/\tTRUE\t0\tLOGIN_INFO\ty")))
    }

    @Test
    fun ordinaryCookiesDoNotCount() {
        assertFalse(AuthCookie.isLoggedIn(listOf(".youtube.com\tTRUE\t/\tTRUE\t0\tPREF\tz")))
        assertFalse(AuthCookie.isLoggedIn(emptyList()))
    }
}
