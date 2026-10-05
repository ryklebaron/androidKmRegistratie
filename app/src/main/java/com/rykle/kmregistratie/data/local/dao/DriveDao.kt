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

    @Query("SELECT * FROM drives WHERE id = :id")
    suspend fun getById(id: Long): DriveEntity?

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

    @Query("SELECT * FROM drives WHERE kmPending = 1 AND endTime IS NOT NULL ORDER BY startTime ASC LIMIT 1")
    suspend fun getOldestKmPendingDrive(): DriveEntity?

    @Query("SELECT * FROM drives WHERE startTime > :startTime ORDER BY startTime ASC LIMIT 1")
    suspend fun getNextDrive(startTime: Long): DriveEntity?

    @Query("UPDATE drives SET endKm = :endKm, kmPending = 0 WHERE id = :id")
    suspend fun resolvePendingKm(id: Long, endKm: Int)

    @Query("SELECT * FROM drives WHERE startTime < :startTime ORDER BY startTime DESC LIMIT 1")
    suspend fun getPreviousDrive(startTime: Long): DriveEntity?

    @Query("""
        UPDATE drives SET
            startKm = :startKm, endKm = :endKm,
            startLat = :startLat, startLng = :startLng, startAddress = :startAddress,
            endLat = :endLat, endLng = :endLng, endAddress = :endAddress,
            kmPending = CASE WHEN :endKm IS NULL THEN kmPending ELSE 0 END
        WHERE id = :id
    """)
    suspend fun updateCorrection(
        id: Long,
        startKm: Int?, endKm: Int?,
        startLat: Double, startLng: Double, startAddress: String?,
        endLat: Double?, endLng: Double?, endAddress: String?,
    )

    @Query("UPDATE drives SET kmPending = 1 WHERE id = :id")
    suspend fun markKmPending(id: Long)

    @Query("UPDATE drives SET hasKmMismatch = :hasKmMismatch WHERE id = :id")
    suspend fun updateKmMismatch(id: Long, hasKmMismatch: Boolean)

    @Query("UPDATE drives SET startAddress = :address WHERE id = :id")
    suspend fun updateStartAddress(id: Long, address: String)

    @Query("UPDATE drives SET endAddress = :address WHERE id = :id")
    suspend fun updateEndAddress(id: Long, address: String)

    @Query("DELETE FROM drives WHERE id = :id")
    suspend fun deleteById(id: Long)
}
