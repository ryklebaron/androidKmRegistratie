package com.rykle.kmregistratie.domain.repository

import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import kotlinx.coroutines.flow.Flow

interface DriveRepository {
    fun getAllDrives(): Flow<List<Drive>>
    suspend fun getActiveDrive(): Drive?
    suspend fun getLatestCompletedDrive(): Drive?
    suspend fun startDrive(
        startTime: Long,
        startLat: Double,
        startLng: Double,
        startAddress: String?,
        driveType: DriveType,
        startKm: Int?,
        hasKmMismatch: Boolean
    ): Long
    suspend fun endDrive(driveId: Long, endTime: Long, endLat: Double, endLng: Double, endAddress: String?, endKm: Int?)
    suspend fun updateStartAddress(driveId: Long, address: String)
    suspend fun updateEndAddress(driveId: Long, address: String)
    suspend fun reopenDrive(driveId: Long)
    suspend fun deleteDrive(driveId: Long)
    suspend fun deleteAllDrives()
    suspend fun getDriveByStartTime(startTime: Long): Drive?
    suspend fun insertFullDrive(
        startTime: Long, endTime: Long?,
        startLat: Double, startLng: Double, startAddress: String?,
        endLat: Double?, endLng: Double?, endAddress: String?,
        driveType: DriveType, startKm: Int?, endKm: Int?, hasKmMismatch: Boolean
    )
}
