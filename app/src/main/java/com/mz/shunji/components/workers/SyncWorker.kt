package com.mz.shunji.components.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.sync.core.Success
import com.mz.shunji.preferences.BackgroundSync
import com.mz.shunji.preferences.PreferenceRepository

class SyncWorker(
    private val preferenceRepository: PreferenceRepository,
    private val noteRepository: NoteRepository,
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        if (preferenceRepository.get<BackgroundSync>().first() == BackgroundSync.DISABLED)
            return@withContext Result.failure()

        when (noteRepository.syncNotes()) {
            Success -> Result.success()
            else -> Result.failure()
        }
    }
}
