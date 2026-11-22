package com.pagelence

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.pagelence.core.lexicon.DictionaryEntry
import com.pagelence.core.lexicon.DictionarySource
import java.io.File

/**
 * Read-only SQLite view of the optional full ECDICT table.
 *
 * Rows carry the core table's fields (`phonetic`, `senses`, `en`, `lemma`,
 * `form`), so [com.pagelence.core.lexicon.WordQuery] can query the shipped core
 * map first and fall through to this table without a second code path. The file
 * is opened lazily: before the reader installs it every lookup simply misses.
 */
class SqliteDictionarySource(private val file: () -> File?) : DictionarySource {
    private var database: SQLiteDatabase? = null
    private var openedPath: String? = null

    @Synchronized
    override fun entry(term: String): DictionaryEntry? {
        val target = file() ?: return null
        if (!target.isFile) {
            closeLocked()
            return null
        }
        val db = openLocked(target) ?: return null
        return db.rawQuery(QUERY, arrayOf(term)).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            DictionaryEntry(
                phonetic = cursor.stringOrNull(0),
                senses = DictionaryEntry.decodeSenses(cursor.stringOrNull(1)),
                en = DictionaryEntry.decodeSenses(cursor.stringOrNull(2)),
                lemma = cursor.stringOrNull(3),
                form = cursor.stringOrNull(4),
            )
        }
    }

    /** Close the handle after the table was deleted or replaced. */
    @Synchronized
    fun close() {
        closeLocked()
    }

    private fun openLocked(target: File): SQLiteDatabase? {
        val path = target.absolutePath
        database?.let { if (openedPath == path) return it }
        closeLocked()
        return runCatching {
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrNull()?.also {
            database = it
            openedPath = path
        }
    }

    private fun closeLocked() {
        runCatching { database?.close() }
        database = null
        openedPath = null
    }

    private fun Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private companion object {
        const val QUERY = "SELECT phonetic, senses, en, lemma, form FROM entries WHERE term = ? LIMIT 1"
    }
}
