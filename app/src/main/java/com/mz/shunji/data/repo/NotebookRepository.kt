package com.mz.shunji.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.mz.shunji.data.dao.NotebookDao
import com.mz.shunji.data.model.Notebook

class NotebookRepository(
    private val notebookDao: NotebookDao,
    private val noteRepository: NoteRepository,
) {

    suspend fun insert(notebook: Notebook): Long {
        return notebookDao.insert(notebook)
    }

    suspend fun delete(vararg notebooks: Notebook) {
        val affectedNotes = notebooks
            .map { noteRepository.getByNotebook(it.id).first() }
            .flatten()
            .filterNot { it.isLocalOnly }

        notebookDao.delete(*notebooks)
    }

    suspend fun update(vararg notebooks: Notebook, shouldSync: Boolean = true) {
        notebookDao.update(*notebooks)
    }

    fun getById(notebookId: Long): Flow<Notebook?> {
        return notebookDao.getById(notebookId)
    }

    fun getAll(): Flow<List<Notebook>> {
        return notebookDao.getAll()
    }

    fun getByName(name: String): Flow<Notebook?> {
        return notebookDao.getByName(name)
    }
}
