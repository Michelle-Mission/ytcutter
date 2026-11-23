package app.ytcutter.core.data

import app.ytcutter.core.data.InfoCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InfoCacheTest {

    @Test
    fun fileNameIsPerVideo() {
        val a = InfoCache.fileName("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        val b = InfoCache.fileName("https://youtu.be/aaaaaaaaaaa")
        assertEquals("info-dQw4w9WgXcQ.json", a)
        assertNotEquals(a, b)
    }
}
