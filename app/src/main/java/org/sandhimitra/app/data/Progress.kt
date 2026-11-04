package org.sandhimitra.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Learning progress in a local SQLite database: lesson completion, practice attempts, bookmarks, engine history. */
class Progress(ctx: Context) : SQLiteOpenHelper(ctx, "progress.db", null, 1) {
    /** Incremented on every write so Compose screens re-read. */
    var version by mutableIntStateOf(0)
        private set

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE lesson(topic TEXT PRIMARY KEY, done INTEGER NOT NULL, ts INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE practice(topic TEXT NOT NULL, item TEXT NOT NULL, correct INTEGER NOT NULL, ts INTEGER NOT NULL, PRIMARY KEY(topic,item))")
        db.execSQL("CREATE TABLE bookmark(topic TEXT PRIMARY KEY, ts INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, input TEXT NOT NULL, ts INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}

    private fun bump() { version++ }

    fun setLessonDone(topic: String, done: Boolean) {
        writableDatabase.replace("lesson", null, ContentValues().apply { put("topic", topic); put("done", if (done) 1 else 0); put("ts", System.currentTimeMillis()) }); bump()
    }
    fun isLessonDone(topic: String): Boolean = readableDatabase.rawQuery("SELECT done FROM lesson WHERE topic=?", arrayOf(topic)).use { it.moveToFirst() && it.getInt(0) == 1 }
    fun doneCount(topics: List<String>): Int = topics.count { isLessonDone(it) }

    fun record(topic: String, item: String, correct: Boolean) {
        writableDatabase.replace("practice", null, ContentValues().apply { put("topic", topic); put("item", item); put("correct", if (correct) 1 else 0); put("ts", System.currentTimeMillis()) }); bump()
    }
    fun attempt(topic: String, item: String): Int? = readableDatabase.rawQuery("SELECT correct FROM practice WHERE topic=? AND item=?", arrayOf(topic, item)).use { if (it.moveToFirst()) it.getInt(0) else null }
    /** (answered, correct) */
    fun stats(topic: String): Pair<Int, Int> = readableDatabase.rawQuery("SELECT COUNT(*), COALESCE(SUM(correct),0) FROM practice WHERE topic=?", arrayOf(topic)).use { it.moveToFirst(); it.getInt(0) to it.getInt(1) }
    fun resetPractice(topic: String) { writableDatabase.delete("practice", "topic=?", arrayOf(topic)); bump() }

    fun setBookmark(topic: String, on: Boolean) {
        if (on) writableDatabase.replace("bookmark", null, ContentValues().apply { put("topic", topic); put("ts", System.currentTimeMillis()) })
        else writableDatabase.delete("bookmark", "topic=?", arrayOf(topic)); bump()
    }
    fun isBookmarked(topic: String) = readableDatabase.rawQuery("SELECT 1 FROM bookmark WHERE topic=?", arrayOf(topic)).use { it.moveToFirst() }

    fun addHistory(kind: String, input: String) {
        val db = writableDatabase
        db.delete("history", "kind=? AND input=?", arrayOf(kind, input))
        db.insert("history", null, ContentValues().apply { put("kind", kind); put("input", input); put("ts", System.currentTimeMillis()) })
        db.execSQL("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 40)"); bump()
    }
    fun history(kind: String): List<String> = readableDatabase.rawQuery("SELECT input FROM history WHERE kind=? ORDER BY id DESC LIMIT 10", arrayOf(kind)).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
    fun clearHistory() { writableDatabase.delete("history", null, null); bump() }
}
