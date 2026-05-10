package com.rykle.kmregistratie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rykle.kmregistratie.data.local.entity.DriveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {

    @Query("SELECT * FROM drives ORDER BY startTime DESC")
    fun getAllDrives(): Flow<List<DriveEntity>>

    @Query("SELECT * FROM drives WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveDrive(): DriveEntity?

    @Query("SELECT * FROM drives WHERE endTime IS NOT NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestCompletedDrive(): DriveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(drive: DriveEntity): Long

    @Query("""
        UPDATE drives
        SET endTime = :endTime, endLat = :endLat, endLng = :endLng, endAddress = :endAddress, endKm = :endKm
        WHERE id = :id
    """)
    suspend fun updateEnd(id: Long, endTime: Long, endLat: Double, endLng: Double, endAddress: String?, endKm: Int?)

    @Query("UPDATE drives SET endTime = NULL, endLat = NULL, endLng = NULL, endKm = NULL WHERE id = :id")
    suspend fun reopenDrive(id: Long)

    @Query("UPDATE drives SET startAddress = :address WHERE id = :id")
    suspend fun updateStartAddress(id: Long, address: String)

    @Query("UPDATE drives SET endAddress = :address WHERE id = :id")
    suspend fun updateEndAddress(id: Long, address: String)

    @Query("DELETE FROM drives WHERE id = :id")
    suspend fun deleteById(id: Long)
}
