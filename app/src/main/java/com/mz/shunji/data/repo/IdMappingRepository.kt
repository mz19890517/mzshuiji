package com.mz.shunji.data.repo

import com.mz.shunji.data.dao.IdMappingDao
import com.mz.shunji.data.model.IdMapping
import com.mz.shunji.preferences.CloudService

class IdMappingRepository(private val idMappingDao: IdMappingDao) {

    suspend fun insert(vararg mappings: IdMapping) = idMappingDao.insert(*mappings)

    suspend fun update(vararg mappings: IdMapping) = idMappingDao.update(*mappings)

    suspend fun assignProviderToNote(mapping: IdMapping) {
        val unassignedMappingId = idMappingDao.getNonRemoteByLocalId(mapping.localNoteId)?.mappingId

        if (unassignedMappingId != null) {
            return idMappingDao.update(
                mapping.copy(mappingId = unassignedMappingId)
            )
        }

        idMappingDao.insert(mapping)
    }

    suspend fun getAllByLocalId(localId: Long) = idMappingDao.getAllByLocalId(localId)

    suspend fun getAllByProvider(provider: CloudService) = idMappingDao.getAllByCloudService(provider)

}
