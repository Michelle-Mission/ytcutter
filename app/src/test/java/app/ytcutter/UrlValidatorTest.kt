package app.ytcutter

import app.ytcutter.core.UrlValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun acceptsYouTubeHosts() {
        assertTrue(UrlValidator.isYouTube("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("  https://youtu.be/dQw4w9WgXcQ  "))
    }

    @Test
    fun rejectsOtherHosts() {
        assertFalse(UrlValidator.isYouTube("https://vimeo.com/22439234"))
        assertFalse(UrlValidator.isYouTube("https://notyoutube.com/watch?v=1"))
        assertFalse(UrlValidator.isYouTube("https://youtube.com.evil.tld/watch?v=1"))
    }

    @Test
    fun rejectsMalformed() {
        assertFalse(UrlValidator.isYouTube(""))
        assertFalse(UrlValidator.isYouTube("bukan url"))
        assertFalse(UrlValidator.isYouTube("youtube.com/watch?v=1"))
        assertFalse(UrlValidator.isYouTube("https://youtu.be/abc def"))
    }
}