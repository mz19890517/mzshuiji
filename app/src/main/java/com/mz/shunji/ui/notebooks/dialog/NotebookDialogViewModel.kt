package com.mz.shunji.ui.notebooks.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.mz.shunji.data.model.Notebook
import com.mz.shunji.data.repo.NotebookRepository

class NotebookDialogViewModel(private val notebookRepository: NotebookRepository) : ViewModel() {
    fun insertNotebook(notebook: Notebook) {
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.insert(notebook)
        }
    }

    fun updateNotebook(notebook: Notebook) {
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.update(notebook)
        }
    }

    suspend fun notebookExistsByName(name: String, ignoreId: Long? = null): Boolean {
        val notebook = notebookRepository.getByName(name).first()
        return notebook != null && (if (ignoreId != null) notebook.id != ignoreId else true)
    }
}
