package org.sandhimitra.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.sandhimitra.app.engine.LexiconApi
import java.io.File

/** Opens the bundled read-only lexicon (copied once from assets to private storage). */
class LexiconDb(ctx: Context) : LexiconApi {
    private val db: SQLiteDatabase
    val headwordCount: Int
    val attestedCount: Int

    init {
        val f = File(ctx.filesDir, "lexicon_v1.db")
        if (!f.exists() || f.length() < 1_000_000) {
            ctx.assets.open("lexicon/lexicon.db").use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        }
        db = SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY)
        headwordCount = scalar("SELECT COUNT(*) FROM lex")
        attestedCount = scalar("SELECT COUNT(*) FROM attested")
    }

    private fun scalar(sql: String) = db.rawQuery(sql, null).use { it.moveToFirst(); it.getInt(0) }
    private fun one(sql: String, v: String): String? = db.rawQuery(sql, arrayOf(v)).use { if (it.moveToFirst()) it.getString(0) else null }
    override fun isHeadword(k1: String) = one("SELECT g FROM lex WHERE k1=?", k1) != null
    override fun isAttested(form: String) = one("SELECT f FROM attested WHERE f=?", form) != null
    override fun closedClass(form: String) = one("SELECT c FROM closed WHERE f=?", form)
}
