package com.mz.shunji.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.mz.shunji.data.sync.nextcloud.NextcloudAPIProvider
import com.mz.shunji.data.sync.nextcloud.ValidateNextcloud

object NextcloudModule {
    val nextcloudModule = module {
        singleOf(::NextcloudAPIProvider)
        singleOf(::ValidateNextcloud)
    }
}
