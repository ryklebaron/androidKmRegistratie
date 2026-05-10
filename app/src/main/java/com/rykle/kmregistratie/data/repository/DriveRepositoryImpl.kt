package com.rykle.kmregistratie.data.repository

import com.rykle.kmregistratie.data.local.dao.DriveDao
import com.rykle.kmregistratie.data.local.entity.DriveEntity
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.repository.DriveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DriveRepositoryImpl @Inject constructor(
    private val driveDao: DriveDao
) : DriveRepository {

    override fun getAllDrives(): Flow<List<Drive>> =
        driveDao.getAllDrives().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveDrive(): Drive? =
        driveDao.getActiveDrive()?.toDomain()

    override suspend fun getLatestCompletedDrive(): Drive? =
        driveDao.getLatestCompletedDrive()?.toDomain()

    override suspend fun startDrive(
        startTime: Long,
        startLat: Double,
        startLng: Double,
        startAddress: String?,
        driveType: DriveType,
        startKm: Int?,
        hasKmMismatch: Boolean
    ): Long = driveDao.insert(
        DriveEntity(
            startTime = startTime,
            endTime = null,
            startLat = startLat,
            startLng = startLng,
            startAddress = startAddress,
            endLat = null,
            endLng = null,
            endAddress = null,
            driveType = driveType.name,
            startKm = startKm,
            endKm = null,
            hasKmMismatch = hasKmMismatch
        )
    )

    override suspend fun endDrive(driveId: Long, endTime: Long, endLat: Double, endLng: Double, endAddress: String?, endKm: Int?) {
        driveDao.updateEnd(driveId, endTime, endLat, endLng, endAddress, endKm)
    }

    override suspend fun updateStartAddress(driveId: Long, address: String) {
        driveDao.updateStartAddress(driveId, address)
    }

    override suspend fun updateEndAddress(driveId: Long, address: String) {
        driveDao.updateEndAddress(driveId, address)
    }

    override suspend fun reopenDrive(driveId: Long) {
        driveDao.reopenDrive(driveId)
    }

    override suspend fun deleteDrive(driveId: Long) {
        driveDao.deleteById(driveId)
    }

    private fun DriveEntity.toDomain() = Drive(
        id = id,
        startTime = startTime,
        endTime = endTime,
        startLat = startLat,
        startLng = startLng,
        startAddress = startAddress,
        endLat = endLat,
        endLng = endLng,
        endAddress = endAddress,
        driveType = DriveType.valueOf(driveType),
        startKm = startKm,
        endKm = endKm,
        hasKmMismatch = hasKmMismatch
    )
}
