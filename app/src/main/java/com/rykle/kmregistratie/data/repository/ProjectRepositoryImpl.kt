package com.rykle.kmregistratie.data.repository

import com.rykle.kmregistratie.data.local.dao.ProjectDao
import com.rykle.kmregistratie.data.local.entity.ProjectEntity
import com.rykle.kmregistratie.domain.model.Project
import com.rykle.kmregistratie.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getProjects(): Flow<List<Project>> =
        projectDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findOrCreate(name: String): Project {
        val trimmed = name.trim()
        projectDao.getByName(trimmed)?.let { return it.toDomain() }
        val id = projectDao.insert(ProjectEntity(name = trimmed, createdAt = System.currentTimeMillis()))
        return Project(id = id, name = trimmed)
    }

    override suspend fun createAtLocation(
        name: String, client: String?, lat: Double?, lng: Double?, address: String?,
    ): Project {
        val entity = ProjectEntity(
            name = name.trim(),
            client = client?.trim()?.ifBlank { null },
            lat = lat?.takeIf { it != 0.0 },
            lng = lng?.takeIf { it != 0.0 },
            address = address?.trim()?.ifBlank { null },
            createdAt = System.currentTimeMillis(),
        )
        return entity.copy(id = projectDao.insert(entity)).toDomain()
    }

    override suspend fun updateDetails(projectId: Long, name: String, client: String?) {
        projectDao.updateDetails(projectId, name.trim(), client?.trim()?.ifBlank { null })
    }

    override suspend fun rename(projectId: Long, name: String) {
        projectDao.rename(projectId, name.trim())
    }

    override suspend fun setArchived(projectId: Long, archived: Boolean) {
        projectDao.setArchived(projectId, archived)
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id, name = name, archived = archived,
        client = client, lat = lat, lng = lng, address = address,
    )
}
