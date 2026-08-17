package com.mz.shunji.ui.common.recycler

import com.mz.shunji.databinding.LayoutNoteBinding

interface NoteRecyclerListener {
    fun onItemClick(position: Int, viewBinding: LayoutNoteBinding)
    fun onLongClick(position: Int, viewBinding: LayoutNoteBinding): Boolean
}
