package app.ytcutter.core

object TimeCode {

    fun parse(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("-")) return null
        val parts = trimmed.split(':')
        if (parts.size !in 1..3) return null
        if (parts.any { it.isEmpty() || it.any { c -> !c.isDigit() } }) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        if (nums.drop(1).any { it !in 0..59 }) return null
        if (nums.size == 2 && nums[0] !in 0..59) return null
        return when (nums.size) {
            1 -> nums[0]
            2 -> nums[0] * 60 + nums[1]
            else -> nums[0] * 3600 + nums[1] * 60 + nums[2]
        }
    }

    fun format(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val r = s % 60
        return if (h > 0) "$h:${two(m)}:${two(r)}" else "${two(m)}:${two(r)}"
    }

    private fun two(v: Int): String = v.toString().padStart(2, '0')
}