package app.ytcutter

import app.ytcutter.data.FileNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {

    @Test
    fun stripsIllegalAndPercent() {
        val name = FileNames.sanitize("a/b\\c%😀:d*e?f\"g<h>i|j")
        listOf('/', '\\', '%', ':', '*', '?', '"', '<', '>', '|').forEach {
            assertFalse("$it harus hilang dari $name", name.contains(it))
        }
        assertTrue(name.startsWith("abc"))
    }

    @Test
    fun keepsEmojiAndSpaces() {
        assertEquals("Lagu Uji 🎵 mantap", FileNames.sanitize("Lagu Uji 🎵 mantap"))
    }

    @Test
    fun truncatesTo80Chars() {
        assertEquals(80, FileNames.sanitize("x".repeat(200)).length)
    }

    @Test
    fun blankFallsBack() {
        assertEquals("video", FileNames.sanitize("   "))
        assertEquals("video", FileNames.sanitize("///"))
    }

    @Test
    fun fileBaseAddsRangeAndExtension() {
        assertEquals("Judul_15-220.mp4", FileNames.displayName("Judul", 15, 220, "mp4"))
        assertEquals("Judul_0-65.m4a", FileNames.displayName("Judul", 0, 65, "m4a"))
    }
}