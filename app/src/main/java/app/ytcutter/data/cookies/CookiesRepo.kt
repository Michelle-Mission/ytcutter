package app.ytcutter.data.cookies

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import app.ytcutter.data.history.HistoryDb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class CookieRow(
    val id: Long,
    val url: String,
    val description: String,
    val content: String,
    val enabled: Boolean,
    val createdAt: Long
)

class CookiesRepo private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val db = HistoryDb(appContext)

    private val _entries = MutableStateFlow<List<CookieRow>>(emptyList())
    val entries: StateFlow<List<CookieRow>> = _entries.asStateFlow()

    init {
        refresh()
    }

    fun insert(url: String, description: String, content: String): Long {
        val values = ContentValues().apply {
            put("url", url)
            put("description", description)
            put("content", content)
            put("enabled", 1)
            put("created_at", System.currentTimeMillis())
        }
        val id = db.writableDatabase.insert("cookies", null, values)
        refresh()
        rebuildFile()
        return id
    }

    fun update(id: Long, url: String, description: String) {
        val values = ContentValues().apply {
            put("url", url)
            put("description", description)
        }
        db.writableDatabase.update("cookies", values, "id = ?", arrayOf(id.toString()))
        refresh()
    }

    fun toggle(id: Long, enabled: Boolean) {
        val values = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        db.writableDatabase.update("cookies", values, "id = ?", arrayOf(id.toString()))
        refresh()
        rebuildFile()
    }

    fun delete(id: Long) {
        db.writableDatabase.delete("cookies", "id = ?", arrayOf(id.toString()))
        refresh()
        rebuildFile()
    }

    fun deleteAll() {
        db.writableDatabase.delete("cookies", null, null)
        refresh()
        rebuildFile()
    }

    fun buildEnabledText(): String = NetscapeFile.build(
        _entries.value.filter { it.enabled }.map { it.content }
    )

    fun enabledCount(): Int = _entries.value.count { it.enabled }

    fun rebuildFile() {
        val text = buildEnabledText()
        val file = File(appContext.filesDir, "cookies.txt")
        if (enabledCount() == 0) {
            file.delete()
        } else {
            file.writeText(text)
        }
    }

    internal fun refresh() {
        val rows = mutableListOf<CookieRow>()
        db.readableDatabase.query(
            "cookies", null, null, null, null, null, "created_at DESC, id DESC"
        ).use { c ->
            while (c.moveToNext()) rows += c.toRow()
        }
        _entries.value = rows
    }

    private fun Cursor.toRow(): CookieRow = CookieRow(
        id = getLong(getColumnIndexOrThrow("id")),
        url = getString(getColumnIndexOrThrow("url")),
        description = getString(getColumnIndexOrThrow("description")),
        content = getString(getColumnIndexOrThrow("content")),
        enabled = getInt(getColumnIndexOrThrow("enabled")) == 1,
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    companion object {
        @Volatile
        private var instance: CookiesRepo? = null

        fun get(context: Context): CookiesRepo = instance ?: synchronized(this) {
            instance ?: CookiesRepo(context.applicationContext).also { instance = it }
        }
    }
}