package com.mz.shunji.di

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import kotlinx.coroutines.DelicateCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.mz.shunji.BuildConfig.VERSION_CODE
import com.mz.shunji.components.MediaStorageManager
import com.mz.shunji.components.backup.BackupManager
import com.mz.shunji.data.AppDatabase
import com.mz.shunji.data.repo.IdMappingRepository
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.repo.NotebookRepository
import com.mz.shunji.data.repo.ReminderRepository
import com.mz.shunji.data.repo.TagRepository
import com.mz.shunji.ui.reminders.ReminderManager

const val TEST_MEDIA_FOLDER = "test_media"

object TestUtilModule {

    // Manual syncModule definition to ensure all dependencies are included
    @OptIn(DelicateCoroutinesApi::class)
    val module = module {
        single {
            MediaStorageManager(
                context = get<Context>(),
                noteRepository = get<NoteRepository>(),
                mediaFolder = TEST_MEDIA_FOLDER
            )
        }
        single {
            ReminderManager(
                context = get<Context>(),
                reminderRepository = get<ReminderRepository>(),
                noteRepository = get<NoteRepository>(),
            )
        }
        single {
            BackupManager(
                currentVersion = VERSION_CODE,
                noteRepository = get<NoteRepository>(),
                notebookRepository = get<NotebookRepository>(),
                tagRepository = get<TagRepository>(),
                reminderRepository = get<ReminderRepository>(),
                idMappingRepository = get<IdMappingRepository>(),
                reminderManager = get<ReminderManager>(),
                context = get<Context>()
            )
        }
        single<AppDatabase> {
            Room.inMemoryDatabaseBuilder(androidContext(), AppDatabase::class.java)
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .addMigrations(AppDatabase.MIGRATION_4_5)
                .build()
        }
        single {
            MigrationTestHelper(
                instrumentation = getInstrumentation(),
                databaseClass = AppDatabase::class.java,
            )
        }
    }
}
