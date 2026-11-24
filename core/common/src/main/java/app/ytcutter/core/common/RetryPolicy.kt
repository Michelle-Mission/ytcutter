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

    private val forbidden = listOf(
        "http error 403",
        "403 forbidden",
        "forbidden",
        "requested format is not available",
        "unable to download video data"
    )

    /** Format pilihan ditolak/diblokir: coba progresif (b) sekali. */
    fun isForbidden(message: String): Boolean {
        if (message.isBlank()) return false
        val lower = message.lowercase()
        return forbidden.any { lower.contains(it) }
    }

    fun isTransient(message: String): Boolean {
        if (message.isBlank()) return false
        val lower = message.lowercase()
        return transient.any { lower.contains(it) }
    }
}
