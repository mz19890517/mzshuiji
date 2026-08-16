package com.notebook.app

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EditNoteActivity : AppCompatActivity() {

    private lateinit var dbHelper: NoteDbHelper
    private var noteId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        dbHelper = NoteDbHelper(this)

        val etTitle = findViewById<EditText>(R.id.et_title)
        val etContent = findViewById<EditText>(R.id.et_content)
        val btnDelete = findViewById<ImageButton>(R.id.btn_delete)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId == -1L) {
            btnDelete.visibility = android.view.View.INVISIBLE
        } else {
            val note = dbHelper.getNote(noteId)
            if (note != null) {
                etTitle.setText(note.title)
                etContent.setText(note.content)
            }
        }

        btnDelete.setOnClickListener {
            if (noteId != -1L) {
                dbHelper.delete(noteId)
                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        findViewById<FloatingActionButton>(R.id.fab_save).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()
            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, R.string.note_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (noteId == -1L) {
                dbHelper.insert(title, content)
            } else {
                dbHelper.update(noteId, title, content)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
    }
}
