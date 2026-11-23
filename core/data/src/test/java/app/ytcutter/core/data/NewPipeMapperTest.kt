package app.ytcutter.core.data

import app.ytcutter.core.data.NewPipeMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class NewPipeMapperTest {

    @Test
    fun mapsAndCleansHeights() {
        val info = NewPipeMapper.toVideoInfo(
            videoId = "dQw4w9WgXcQ",
            title = "Judul",
            durationSec = 212,
            thumbnail = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hq.jpg",
            heights = listOf(360, 1080, 144, 720, 1080, 90),
            hasAudio = true
        )
        assertEquals("dQw4w9WgXcQ", info.id)
        assertEquals("Judul", info.title)
        assertEquals(212, info.durationSec)
        assertEquals(listOf(1080, 720, 360, 144), info.heights.toList())
        assertEquals(true, info.hasAudio)
    }

    @Test
    fun keepsAudioOnlyVideo() {
        val info = NewPipeMapper.toVideoInfo("abc12345678", "Podcast", 60, null, emptyList(), true)
        assertEquals(emptySet<Int>(), info.heights)
        assertEquals(true, info.hasAudio)
        assertEquals(null, info.thumbnailUrl)
    }
}
