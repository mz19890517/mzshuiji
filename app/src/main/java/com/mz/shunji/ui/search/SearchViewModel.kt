package com.mz.shunji.ui.search

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.mz.shunji.data.model.Note
import com.mz.shunji.data.model.Notebook
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.repo.NotebookRepository
import com.mz.shunji.data.sync.core.BackendProvider
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.preferences.SortMethod
import com.mz.shunji.ui.common.AbstractNotesViewModel

class SearchViewModel(
    noteRepository: NoteRepository,
    notebookRepository: NotebookRepository,
    preferenceRepository: PreferenceRepository,
    backendProvider: BackendProvider,
) : AbstractNotesViewModel(preferenceRepository, backendProvider) {
    private val searchKeyData: MutableStateFlow<String> = MutableStateFlow("")

    var isFirstLoad = true

    override val provideNotes = { sortMethod: SortMethod ->
        notebookRepository.getAll().distinctUntilChanged().flatMapLatest { notebooks ->
            searchKeyData.debounce(300).flatMapLatest { searchKey ->
                noteRepository
                    .getAll(sortMethod)
                    .map { notes ->
                        getSearchResults(
                            searchKey.trim(),
                            notes,
                            notebooks
                        )
                    }
            }
        }
    }

    private fun getSearchResults(
        searchKey: String,
        notes: List<Note>,
        notebooks: List<Notebook>,
    ): List<Note> = notes.filter { note ->
        fun String.matches(): Boolean = contains(searchKey, true)

        when (note.isList) {
            true -> note.taskList.any { it.content.matches() }
            false -> note.content.matches()
        } ||
            note.title.matches() ||
            note.attachments.any { it.description.matches() } ||
            note.tags.any { it.name.matches() } ||
            notebooks.any { it.name.matches() && it.id == note.notebookId }
    }

    fun setSearchQuery(query: String) = viewModelScope.launch {
        searchKeyData.emit(query)
    }
}
