package app.ytcutter.core.common

import app.ytcutter.core.common.FormatMapper
import app.ytcutter.core.model.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatMapperTest {

    @Test
    fun listsHeightsDescendingThenAudio() {
        val options = FormatMapper.options(setOf(720, 2160, 1080, 480), hasAudio = true)
        assertEquals(
            listOf("2160p", "1080p", "720p", "480p", "Audio"),
            options.map { it.label }
        )
        assertEquals(listOf(2160, 1080, 720, 480, null), options.map { it.height })
        assertEquals(Quality("Audio", null), options.last())
    }

    @Test
    fun dropsUnusableHeights() {
        val options = FormatMapper.options(setOf(100, 480), hasAudio = false)
        assertEquals(listOf("480p"), options.map { it.label })
    }

    @Test
    fun emptyWhenNothingUsable() {
        assertTrue(FormatMapper.options(emptySet(), hasAudio = false).isEmpty())
    }

    @Test
    fun audioOnly() {
        val options = FormatMapper.options(emptySet(), hasAudio = true)
        assertEquals(listOf("Audio"), options.map { it.label })
    }

    @Test
    fun videoSelectorCapsHeight() {
        assertEquals(
            "bv*[height<=1080]+ba/b[height<=1080]/b",
            FormatMapper.videoSelector(1080)
        )
    }

    @Test
    fun fallbackSelectorPrefersProgressiveMp4() {
        assertEquals("b[height<=720][ext=mp4]/b[height<=720]/b", FormatMapper.fallbackSelector(720))
    }

    @Test
    fun audioSelector() {
        assertEquals("ba/b", FormatMapper.audioSelector())
    }
}