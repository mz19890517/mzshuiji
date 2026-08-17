package com.mz.shunji.data.repo

import kotlinx.coroutines.flow.Flow
import me.msoul.datastore.defaultOf
import com.mz.shunji.data.model.IdMapping
import com.mz.shunji.data.model.Note
import com.mz.shunji.data.sync.core.BaseResult
import com.mz.shunji.preferences.CloudService
import com.mz.shunji.preferences.SortMethod

interface NoteRepository {
    suspend fun insertNote(note: Note, sync: Boolean = true): Long
    suspend fun updateNotes(vararg notes: Note, sync: Boolean = true)
    suspend fun moveNotesToBin(vararg notes: Note, sync: Boolean = true)
    suspend fun restoreNotes(vararg notes: Note)
    suspend fun deleteNotes(vararg notes: Note, sync: Boolean = true)
    suspend fun discardEmptyNotes(): Boolean
    suspend fun permanentlyDeleteNotesInBin()

    suspend fun syncNotes(): BaseResult
    fun getById(noteId: Long): Flow<Note?>
    fun getDeleted(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getArchived(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getNonDeleted(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getNonDeletedOrArchived(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getAll(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getByNotebook(notebookId: Long, sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getNonRemoteNotes(provider: CloudService, sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    fun getNotesWithoutNotebook(sortMethod: SortMethod = defaultOf()): Flow<List<Note>>
    suspend fun getNotesByCloudService(provider: CloudService): Map<IdMapping, Note?>
    suspend fun deleteIdMappingsForCloudService(cloudService: CloudService)
}
