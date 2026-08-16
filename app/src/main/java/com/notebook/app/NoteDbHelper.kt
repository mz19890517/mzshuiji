package com.notebook.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NoteDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun allNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            null, null, null, null, null,
            "$COLUMN_UPDATED_AT DESC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(COLUMN_ID)
            val titleCol = it.getColumnIndexOrThrow(COLUMN_TITLE)
            val contentCol = it.getColumnIndexOrThrow(COLUMN_CONTENT)
            val updatedCol = it.getColumnIndexOrThrow(COLUMN_UPDATED_AT)
            while (it.moveToNext()) {
                notes.add(
                    Note(
                        id = it.getLong(idCol),
                        title = it.getString(titleCol),
                        content = it.getString(contentCol),
                        updatedAt = it.getLong(updatedCol)
                    )
                )
            }
        }
        return notes
    }

    fun getNote(id: Long): Note? {
        val cursor = readableDatabase.query(
            TABLE_NOTES,
            null, "$COLUMN_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return Note(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE)),
                    content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT)),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                )
            }
        }
        return null
    }

    fun insert(title: String, content: String): Long {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_NOTES, null, values)
    }

    fun update(id: Long, title: String, content: String) {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_CONTENT, content)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        writableDatabase.update(
            TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(id.toString())
        )
    }

    fun delete(id: Long) {
        writableDatabase.delete(
            TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id.toString())
        )
    }

    companion object {
        const val DATABASE_NAME = "notebook.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NOTES = "notes"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_UPDATED_AT = "updated_at"
    }
}
