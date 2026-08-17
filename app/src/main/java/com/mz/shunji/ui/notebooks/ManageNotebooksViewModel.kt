package com.mz.shunji.ui.notebooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.mz.shunji.data.model.Notebook
import com.mz.shunji.data.repo.NotebookRepository
import com.mz.shunji.preferences.PreferenceRepository

class ManageNotebooksViewModel(
    private val notebookRepository: NotebookRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    fun deleteNotebooks(vararg notebooks: Notebook) {
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.delete(*notebooks)
        }
    }

    fun getSortNavdrawerNotebooksMethod(): String {
        return runBlocking {
            preferenceRepository.getAll().first().sortNavdrawerNotebooksMethod.name
        }
    }
}
