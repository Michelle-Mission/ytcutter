package app.ytcutter.work

/** Proses yt-dlp yang sedang berjalan dan item yang diminta batal. */
object ActiveWork {
    @Volatile
    var processId: String? = null

    @Volatile
    var cancelId: Long? = null
}