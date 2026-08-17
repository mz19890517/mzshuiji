package com.mz.shunji.ui.deleted

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mz.shunji.components.MediaStorageManager
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.sync.core.BackendProvider
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.ui.common.AbstractNotesViewModel

class DeletedViewModel(
    private val notesRepository: NoteRepository,
    private val mediaStorageManager: MediaStorageManager,
    preferenceRepository: PreferenceRepository,
    backendProvider: BackendProvider,
) : AbstractNotesViewModel(preferenceRepository, backendProvider) {

    override val provideNotes = notesRepository::getDeleted

    fun permanentlyDeleteNotesInBin() {
        viewModelScope.launch(Dispatchers.IO) {
            notesRepository.permanentlyDeleteNotesInBin()
            mediaStorageManager.cleanUpStorage()
        }
    }
}
