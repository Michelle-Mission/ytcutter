package app.ytcutter.core.data.work

/** Proses yt-dlp yang sedang berjalan dan item yang diminta batal. */
object ActiveWork {
    @Volatile
    var processId: String? = null

    @Volatile
    var runningId: Long? = null

    @Volatile
    var cancelId: Long? = null
}

object CancelRules {
    fun shouldKill(targetId: Long, runningId: Long?): Boolean = runningId == targetId
}
