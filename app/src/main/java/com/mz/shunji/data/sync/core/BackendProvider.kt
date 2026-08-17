package com.mz.shunji.data.sync.core

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.mz.shunji.data.sync.fs.StorageBackend
import com.mz.shunji.data.sync.fs.StorageConfig
import com.mz.shunji.data.sync.nextcloud.NextcloudAPIProvider
import com.mz.shunji.data.sync.nextcloud.NextcloudBackend
import com.mz.shunji.data.sync.nextcloud.NextcloudConfig
import com.mz.shunji.di.SyncScope
import com.mz.shunji.preferences.AppPreferences
import com.mz.shunji.preferences.CloudService
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.ui.utils.ConnectionManager

class BackendProvider(
    private val context: Context,
    private val nextcloudApiProvider: NextcloudAPIProvider,
    preferenceRepository: PreferenceRepository,
    syncingScope: SyncScope,
    private val connectionManager: ConnectionManager,
) {
    private val syncService: Flow<CloudService> = preferenceRepository.getAll().map { it.cloudService }
    private val pref: StateFlow<AppPreferences?> =
        preferenceRepository.getAll().stateIn(syncingScope, SharingStarted.Eagerly, null)

    val syncProvider: StateFlow<ISyncBackend?> = combine(
        syncService,
        NextcloudConfig.fromPreferences(preferenceRepository),
        StorageConfig.storageLocation(preferenceRepository)
    ) { service, nextcloudConfig, storageConfig ->
        when (service) {
            CloudService.DISABLED -> null
            CloudService.NEXTCLOUD -> nextcloudConfig?.let { NextcloudBackend(nextcloudApiProvider, it) }
            CloudService.FILE_STORAGE -> storageConfig?.let { StorageBackend(context, it) }
        }
    }.stateIn(syncingScope, SharingStarted.Eagerly, null)

    val isSyncing: Boolean
        get() = syncProvider.value != null && connectionManager.isConnectionAvailable(
            syncMode = pref.value?.syncMode,
            cloudService = syncProvider.value?.type
        )
}
