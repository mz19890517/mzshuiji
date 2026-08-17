package com.mz.shunji.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.mz.shunji.data.sync.core.BackendProvider
import com.mz.shunji.data.sync.core.ProcessRemoteActions
import com.mz.shunji.data.sync.core.SynchronizeNotes

object SyncModule {

    val syncModule = module {
        single<SyncScope> { SyncScope(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) }

        singleOf(::BackendProvider)
        singleOf(::ProcessRemoteActions)
        singleOf(::SynchronizeNotes)
    }
}

class SyncScope(scope: CoroutineScope) : CoroutineScope by scope
