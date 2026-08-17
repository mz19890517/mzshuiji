package com.mz.shunji

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import com.mz.shunji.di.MarkwonModule
import com.mz.shunji.di.NextcloudModule
import com.mz.shunji.di.PreferencesModule
import com.mz.shunji.di.RepositoryModule
import com.mz.shunji.di.SyncModule
import com.mz.shunji.di.TestUtilModule
import com.mz.shunji.di.UIModule
import com.mz.shunji.di.UtilModule

class TestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }
}

@OptIn(KoinExperimentalAPI::class)
class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@TestApplication)
            modules(
                TestUtilModule.module,
                MarkwonModule.markwonModule,
                NextcloudModule.nextcloudModule,
                PreferencesModule.prefModule,
                RepositoryModule.repoModule,
                SyncModule.syncModule,
                UIModule.uiModule,
                UtilModule.utilModule,
            )
        }
    }
}
