package app.ytcutter

import app.ytcutter.data.MetadataParser
import app.ytcutter.data.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataParserTest {

    private fun fixture(): String =
        javaClass.classLoader!!.getResource("info_1080.json")!!.readText()

    @Test
    fun parsesIdentityFields() {
        val info = MetadataParser.parse(fixture())!!
        assertEquals("dQw4w9WgXcQ", info.id)
        assertEquals("Lagu Uji 🎵 100% / mantap", info.title)
        assertEquals(761, info.durationSec)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg", info.thumbnailUrl)
    }

    @Test
    fun collectsVideoHeights() {
        val info = MetadataParser.parse(fixture())!!
        assertEquals(setOf(1080, 720), info.heights)
    }

    @Test
    fun detectsAudio() {
        val info = MetadataParser.parse(fixture())!!
        assertTrue(info.hasAudio)
    }

    @Test
    fun malformedJsonReturnsNull() {
        assertNull(MetadataParser.parse("bukan json"))
        assertNull(MetadataParser.parse(""))
    }

    @Test
    fun missingDurationBecomesZero() {
        val info = MetadataParser.parse("""{"id":"x","title":"t","formats":[]}""")!!
        assertEquals(VideoInfo("x", "t", 0, null, emptySet(), false), info)
    }
}