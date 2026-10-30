package com.rykle.kmregistratie.domain.repository

import com.rykle.kmregistratie.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    /** Bestaand project met deze naam (hoofdletterongevoelig), anders een nieuw. */
    suspend fun findOrCreate(name: String): Project
    /** Nieuw project op een vaste plek (klantlocatie). */
    suspend fun createAtLocation(name: String, client: String?, lat: Double?, lng: Double?, address: String?): Project
    suspend fun updateDetails(projectId: Long, name: String, client: String?)
    suspend fun rename(projectId: Long, name: String)
    suspend fun setArchived(projectId: Long, archived: Boolean)
}
