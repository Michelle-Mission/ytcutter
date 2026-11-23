package app.ytcutter.core.data.cookies

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.ytcutter.core.common.NetscapeFile
import java.io.File

/**
 * Baca database cookie WebView langsung supaya cookie HttpOnly (SID/HSID/SSID)
 * ikut tertangkap; CookieManager.getCookie hanya mengembalikan cookie non-HttpOnly.
 */
object WebViewCookieReader {

    data class RawCookie(
        val host: String,
        val name: String,
        val value: String,
        val path: String,
        val secure: Boolean,
        val expiry: Long,
        val httpOnly: Boolean
    )

    fun export(
        context: Context,
        hostSuffixes: List<String> = listOf("youtube.com", "google.com")
    ): String? {
        val cookieDb = findCookiesDb(context) ?: return null
        val snapshot = snapshot(context, cookieDb) ?: return null
        val rows = mutableListOf<RawCookie>()
        runCatching {
            SQLiteDatabase.openDatabase(snapshot.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                .use { db ->
                    val selection = hostSuffixes.joinToString(" OR ") { "host_key LIKE ?" }
                    val args = hostSuffixes.map { "%$it" }.toTypedArray()
                    db.query(
                        "cookies",
                        arrayOf("host_key", "name", "value", "path", "is_secure", "expires_utc", "is_httponly"),
                        selection,
                        args,
                        null,
                        null,
                        null
                    ).use { c ->
                        while (c.moveToNext()) {
                            rows += RawCookie(
                                host = c.getString(0),
                                name = c.getString(1),
                                value = c.getString(2) ?: "",
                                path = c.getString(3) ?: "/",
                                secure = c.getLong(4) == 1L,
                                expiry = c.getLong(5),
                                httpOnly = c.getLong(6) == 1L
                            )
                        }
                    }
                }
        }
        snapshot.delete()
        if (rows.isEmpty()) return null
        return NetscapeFile.build(listOf(toLines(rows).joinToString("\n")))
    }

    internal fun toLines(rows: List<RawCookie>): List<String> = rows.map { row ->
        val host = if (row.host.startsWith(".")) row.host else ".${row.host}"
        val prefix = if (row.httpOnly) "#HttpOnly_" else ""
        "$prefix$host\tTRUE\t${row.path}\t${if (row.secure) "TRUE" else "FALSE"}" +
            "\t${row.expiry}\t${row.name}\t${row.value}"
    }

    private fun findCookiesDb(context: Context): File? =
        File(context.applicationInfo.dataDir).walkTopDown()
            .firstOrNull { it.name == "Cookies" && it.path.contains("app_webview") }

    private fun snapshot(context: Context, source: File): File? = runCatching {
        val dir = File(context.cacheDir, "webview-cookies")
        dir.mkdirs()
        val target = File(dir, "Cookies.snapshot")
        listOf("", "-wal", "-shm").forEach { suffix ->
            val from = File(source.absolutePath + suffix)
            if (from.exists()) from.copyTo(File(target.absolutePath + suffix), overwrite = true)
        }
        target.takeIf { it.exists() }
    }.getOrNull()
}