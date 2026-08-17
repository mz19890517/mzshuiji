package com.mz.shunji.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.msoul.datastore.EnumPreference
import com.mz.shunji.data.repo.NoteRepository
import com.mz.shunji.data.sync.nextcloud.NextcloudConfig
import com.mz.shunji.preferences.CloudService
import com.mz.shunji.preferences.PreferenceRepository
import com.mz.shunji.preferences.SyncMode

class SettingsViewModel(
    private val preferenceRepository: PreferenceRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    val appPreferences = preferenceRepository.getAll()
    val loggedInUsername = NextcloudConfig.fromPreferences(preferenceRepository).map { it?.username }

    fun <T> setPreference(pref: T) where T : Enum<T>, T : EnumPreference {
        when (pref) {
            is CloudService -> {
                if (pref in listOf(CloudService.FILE_STORAGE, CloudService.DISABLED)) {
                    setPreference(SyncMode.ALWAYS)
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.set(pref)
        }
    }

    suspend fun <T> setPreferenceSuspending(pref: T) where T : Enum<T>, T : EnumPreference {
        preferenceRepository.set(pref)
    }

    fun getEncryptedString(key: String): Flow<String> {
        return preferenceRepository.getEncryptedString(key)
    }

    fun setEncryptedString(key: String, value: String) =
        viewModelScope.launch { preferenceRepository.putEncryptedStrings(key to value) }

    fun clearNextcloudCredentials() = viewModelScope.launch {
        preferenceRepository.putEncryptedStrings(
            PreferenceRepository.NEXTCLOUD_INSTANCE_URL to "",
            PreferenceRepository.NEXTCLOUD_PASSWORD to "",
            PreferenceRepository.NEXTCLOUD_USERNAME to "",
        )
        noteRepository.deleteIdMappingsForCloudService(CloudService.NEXTCLOUD)
    }

    /**
     * Removes all IdMappings for file storage if the current cloud service is FILE_STORAGE
     * and the new storage location is different from the previous one.
     *
     * @param newLocation The new storage location URI as a string
     * @param previousLocation The previous storage location URI as a string
     */
    fun removeFileStorageIdMappingsIfNeeded(newLocation: String, previousLocation: String) {
        if (newLocation != previousLocation) {
            viewModelScope.launch(Dispatchers.IO) {
                val currentCloudService = preferenceRepository.getAll().first().cloudService
                if (currentCloudService == CloudService.FILE_STORAGE) {
                    noteRepository.deleteIdMappingsForCloudService(CloudService.FILE_STORAGE)
                }
            }
        }
    }
}
