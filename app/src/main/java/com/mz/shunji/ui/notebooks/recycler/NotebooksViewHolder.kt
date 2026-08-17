package com.mz.shunji.ui.notebooks.recycler

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.mz.shunji.data.model.Notebook
import com.mz.shunji.databinding.LayoutNotebookBinding
import com.mz.shunji.ui.common.recycler.SelectableViewHolder

class NotebooksViewHolder(
    val context: Context,
    val binding: LayoutNotebookBinding,
    listener: NotebooksRecyclerListener?,
) : RecyclerView.ViewHolder(binding.root), SelectableViewHolder {

    init {
        if (listener != null) {
            itemView.setOnClickListener { listener.onItemClick(bindingAdapterPosition) }
            itemView.setOnLongClickListener { listener.onLongClick(bindingAdapterPosition) }
        }
    }

    private fun setName(name: String) {
        binding.textViewNotebookName.text = name
    }

    fun bind(notebook: Notebook) {
        setName(notebook.name)
    }

    fun runPayloads(notebook: Notebook, payloads: List<NotebooksRecyclerAdapter.Payload>) {
        payloads.forEach {
            when (it) {
                NotebooksRecyclerAdapter.Payload.NameChanged -> setName(notebook.name)
            }
        }
    }

    override fun onSelectedStatusChanged(isSelected: Boolean) {
        binding.root.isSelected = isSelected
    }
}
