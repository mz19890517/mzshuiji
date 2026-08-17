package com.mz.shunji.components.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.mz.shunji.components.MediaStorageManager
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.preferences.NoteDeletionTime
import com.mz.shunji.preferences.PreferenceRepository
import java.time.Instant

class BinCleaningWorker(
    context: Context,
    params: WorkerParameters,
    private val preferenceRepository: PreferenceRepository,
    private val noteRepository: NoteRepository,
    private val mediaStorageManager: MediaStorageManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val deletionTime = preferenceRepository.get<NoteDeletionTime>().first().interval.takeIf { it > 0 }
            ?: return@withContext Result.success()
        val now = Instant.now()
        val toBeDeleted = noteRepository.getDeleted().first()
            .filter { note ->
                val deletionDate = note.deletionDate?.let { Instant.ofEpochSecond(it) } ?: return@filter false
                now.isAfter(deletionDate.plusSeconds(deletionTime))
            }
            .toTypedArray()

        noteRepository.deleteNotes(*toBeDeleted)
        mediaStorageManager.cleanUpStorage()

        Result.success()
    }
}
