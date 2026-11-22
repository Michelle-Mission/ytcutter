package app.ytcutter

import app.ytcutter.core.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun acceptsSingleVideos() {
        assertTrue(UrlValidator.isYouTube("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL123"))
        assertTrue(UrlValidator.isYouTube("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://youtu.be/dQw4w9WgXcQ?t=42"))
        assertTrue(UrlValidator.isYouTube("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isYouTube("https://www.youtube.com/shorts/abc123"))
        assertTrue(UrlValidator.isYouTube("https://www.youtube.com/live/abc123"))
        assertTrue(UrlValidator.isYouTube("https://www.youtube.com/embed/abc123"))
        assertTrue(UrlValidator.isYouTube("  https://youtu.be/dQw4w9WgXcQ  "))
    }

    @Test
    fun rejectsPlaylistsChannelsAndOtherPages() {
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/playlist?list=PL123"))
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/channel/UC123"))
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/@someone"))
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/results?search_query=x"))
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/feed/subscriptions"))
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/watch"))
        assertFalse(UrlValidator.isYouTube("https://www.youtube.com/watch?list=PL123"))
        assertFalse(UrlValidator.isYouTube("https://youtu.be/"))
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

    @Test
    fun extractsVideoId() {
        assertEquals("dQw4w9WgXcQ", UrlValidator.videoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1"))
        assertEquals("dQw4w9WgXcQ", UrlValidator.videoId("https://youtu.be/dQw4w9WgXcQ?t=42"))
        assertEquals("dQw4w9WgXcQ", UrlValidator.videoId("https://m.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", UrlValidator.videoId("https://www.youtube.com/live/dQw4w9WgXcQ"))
        assertNull(UrlValidator.videoId("https://www.youtube.com/watch"))
        assertNull(UrlValidator.videoId("https://vimeo.com/22439234"))
    }
}
