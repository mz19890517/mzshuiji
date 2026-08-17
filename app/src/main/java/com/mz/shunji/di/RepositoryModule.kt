package com.mz.shunji.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import com.mz.shunji.data.repo.IdMappingRepository
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.repo.NoteRepositoryImpl
import com.mz.shunji.data.repo.NotebookRepository
import com.mz.shunji.data.repo.ReminderRepository
import com.mz.shunji.data.repo.TagRepository

object RepositoryModule {

    val repoModule = module {
        includes(DatabaseModule.dbModule)

        singleOf(::NoteRepositoryImpl) bind NoteRepository::class
        singleOf(::ReminderRepository)
        singleOf(::NotebookRepository)
        singleOf(::TagRepository)
        singleOf(::IdMappingRepository)
    }
}
