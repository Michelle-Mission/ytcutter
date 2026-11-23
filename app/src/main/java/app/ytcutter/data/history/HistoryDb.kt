package app.ytcutter.data.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "ytcutter.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE downloads (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                video_id TEXT,
                url TEXT NOT NULL,
                title TEXT NOT NULL,
                quality_label TEXT NOT NULL,
                height INTEGER,
                start_sec INTEGER NOT NULL DEFAULT 0,
                end_sec INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL,
                progress INTEGER NOT NULL DEFAULT 0,
                detail TEXT,
                error TEXT,
                error_action TEXT NOT NULL DEFAULT 'NONE',
                uri TEXT,
                file_name TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_downloads_status ON downloads(status, created_at)")
        db.execSQL(
            """
            CREATE TABLE cookies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                content TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS downloads")
        db.execSQL("DROP TABLE IF EXISTS cookies")
        onCreate(db)
    }
}