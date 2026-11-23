package app.ytcutter.core.common

object Range {

    fun isSelectable(durationSec: Int): Boolean = durationSec >= 3

    fun clamp(start: Int, end: Int, durationSec: Int): Pair<Int, Int> {
        val duration = durationSec.coerceAtLeast(0)
        if (duration <= 2) return 0 to duration
        val latestStart = duration - 2
        val s = start.coerceIn(0, latestStart)
        val e = end.coerceIn(s + 2, duration)
        return s to e
    }
}