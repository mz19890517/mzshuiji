package com.mz.shunji.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.mz.shunji.ui.ActivityViewModel
import com.mz.shunji.ui.archive.ArchiveViewModel
import com.mz.shunji.ui.attachments.dialog.AttachmentDialogViewModel
import com.mz.shunji.ui.deleted.DeletedViewModel
import com.mz.shunji.ui.editor.EditorViewModel
import com.mz.shunji.ui.launcher.LauncherViewModel
import com.mz.shunji.ui.main.MainViewModel
import com.mz.shunji.ui.notebooks.ManageNotebooksViewModel
import com.mz.shunji.ui.notebooks.dialog.NotebookDialogViewModel
import com.mz.shunji.ui.reminders.EditReminderViewModel
import com.mz.shunji.ui.search.SearchViewModel
import com.mz.shunji.ui.settings.SettingsViewModel
import com.mz.shunji.ui.sync.nextcloud.NextcloudViewModel
import com.mz.shunji.ui.tags.TagsViewModel
import com.mz.shunji.ui.tags.dialog.TagDialogViewModel

object UIModule {
    val uiModule = module {
        viewModelOf(::EditorViewModel)
        viewModelOf(::ActivityViewModel)
        viewModelOf(::ArchiveViewModel)
        viewModelOf(::TagsViewModel)
        viewModelOf(::TagDialogViewModel)
        viewModelOf(::NextcloudViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::SearchViewModel)
        viewModelOf(::EditReminderViewModel)
        viewModelOf(::ManageNotebooksViewModel)
        viewModelOf(::NotebookDialogViewModel)
        viewModelOf(::MainViewModel)
        viewModel { LauncherViewModel(androidApplication(), get()) }
        viewModelOf(::DeletedViewModel)
        viewModelOf(::AttachmentDialogViewModel)
    }
}
