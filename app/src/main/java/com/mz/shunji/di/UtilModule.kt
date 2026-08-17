package com.mz.shunji.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.mz.shunji.App
import com.mz.shunji.BuildConfig
import com.mz.shunji.components.MediaStorageManager
import com.mz.shunji.components.backup.BackupManager
import com.mz.shunji.components.workers.BinCleaningWorker
import com.mz.shunji.components.workers.SyncWorker
import com.mz.shunji.ui.reminders.ReminderManager
import com.mz.shunji.ui.utils.ConnectionManager
import com.mz.shunji.ui.utils.Toaster

object UtilModule {

    val utilModule = module {
        includes(RepositoryModule.repoModule, SyncModule.syncModule)

        workerOf(::BinCleaningWorker)
        workerOf(::SyncWorker)

        single {
            MediaStorageManager(
                context = androidContext(),
                noteRepository = get(),
                mediaFolder = App.MEDIA_FOLDER
            )
        }

        single {
            BackupManager(
                BuildConfig.VERSION_CODE,
                noteRepository = get(),
                notebookRepository = get(),
                tagRepository = get(),
                reminderRepository = get(),
                idMappingRepository = get(),
                reminderManager = get(),
                context = androidContext(),
            )
        }
        singleOf(::ReminderManager)
        singleOf(::ConnectionManager)
        singleOf(::Toaster)
    }
}
