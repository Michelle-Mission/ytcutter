package app.ytcutter.core.common

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {

    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(raw: String?): Version? {
            val text = raw?.trim()?.removePrefix("v")?.removePrefix("V") ?: return null
            val parts = text.split('.')
            if (parts.size < 2 || parts.size > 3) return null
            val nums = parts.map { it.toIntOrNull() ?: return null }
            return Version(nums[0], nums[1], nums.getOrElse(2) { 0 })
        }
    }
}
