package app.ytcutter.core.common

import app.ytcutter.core.model.Quality

object FormatMapper {

    private const val MIN_VIDEO_HEIGHT = 144

    fun options(heights: Set<Int>, hasAudio: Boolean): List<Quality> {
        val video = heights
            .filter { it >= MIN_VIDEO_HEIGHT }
            .distinct()
            .sortedDescending()
            .map { Quality("${it}p", it) }
        return if (hasAudio) video + Quality("Audio", null) else video
    }

    fun videoSelector(height: Int): String =
        "bv*[height<=$height]+ba/b[height<=$height]/b"

    fun audioSelector(): String = "ba/b"
}