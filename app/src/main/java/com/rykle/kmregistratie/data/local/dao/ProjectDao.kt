package com.rykle.kmregistratie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rykle.kmregistratie.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY archived ASC, name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): ProjectEntity?

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Query("UPDATE projects SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE projects SET name = :name, client = :client WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, client: String?)

    @Query("UPDATE projects SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}
