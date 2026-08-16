package com.notebook.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: NoteDbHelper
    private lateinit var adapter: NoteAdapter
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = NoteDbHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_notes)
        emptyView = findViewById(R.id.tv_empty)

        adapter = NoteAdapter(emptyList()) { note ->
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, EditNoteActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        val notes = dbHelper.allNotes()
        adapter.submitList(notes)
        emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }
}
