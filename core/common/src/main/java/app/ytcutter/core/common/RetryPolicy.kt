package app.ytcutter.core.common

object RetryPolicy {

    private val transient = listOf(
        "timed out",
        "connection reset",
        "connection aborted",
        "unable to download",
        "http error 5",
        "temporary failure",
        "network is unreachable"
    )

    fun isTransient(message: String): Boolean {
        if (message.isBlank()) return false
        val lower = message.lowercase()
        return transient.any { lower.contains(it) }
    }
}
