package com.mz.shunji.ui.archive

import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.sync.core.BackendProvider
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.ui.common.AbstractNotesViewModel

class ArchiveViewModel(
    noteRepository: NoteRepository,
    preferenceRepository: PreferenceRepository,
    backendProvider: BackendProvider
) : AbstractNotesViewModel(preferenceRepository, backendProvider) {
    override val provideNotes = noteRepository::getArchived
}
