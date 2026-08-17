package com.mz.shunji.ui.main

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import com.mz.shunji.R
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.repo.NotebookRepository
import com.mz.shunji.data.sync.core.BackendProvider
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.preferences.SortMethod
import com.mz.shunji.ui.common.AbstractNotesViewModel

class MainViewModel(
    private val noteRepository: NoteRepository,
    private val notebookRepository: NotebookRepository,
    preferenceRepository: PreferenceRepository,
    backendProvider: BackendProvider,
) : AbstractNotesViewModel(preferenceRepository, backendProvider) {

    private val notebookIdFlow: MutableStateFlow<Long?> = MutableStateFlow(null)

    override val provideNotes = { sortMethod: SortMethod ->
        notebookIdFlow.flatMapLatest { id ->
            when (id) {
                null -> noteRepository.getNonDeletedOrArchived(sortMethod)
                R.id.nav_default_notebook.toLong() -> noteRepository.getNotesWithoutNotebook(sortMethod)
                else -> noteRepository.getByNotebook(id, sortMethod)
            }
        }
    }

    suspend fun notebookExists(notebookId: Long) = notebookRepository.getById(notebookId).firstOrNull() != null

    fun initialize(notebookId: Long?) {
        viewModelScope.launch { notebookIdFlow.emit(notebookId) }
    }
}
