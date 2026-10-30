package com.rykle.kmregistratie.data.repository

import com.rykle.kmregistratie.data.local.dao.DriveDao
import com.rykle.kmregistratie.data.local.entity.DriveEntity
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveCorrection
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

    override suspend fun getDriveById(driveId: Long): Drive? =
        driveDao.getById(driveId)?.toDomain()

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
        hasKmMismatch: Boolean,
        projectId: Long?,
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
            hasKmMismatch = hasKmMismatch,
            projectId = projectId,
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

    override suspend fun getOldestKmPendingDrive(): Drive? =
        driveDao.getOldestKmPendingDrive()?.toDomain()

    override suspend fun getNextDrive(after: Drive): Drive? =
        driveDao.getNextDrive(after.startTime)?.toDomain()

    override suspend fun getPreviousDrive(before: Drive): Drive? =
        driveDao.getPreviousDrive(before.startTime)?.toDomain()

    override suspend fun applyCorrection(driveId: Long, correction: DriveCorrection) {
        with(correction) {
            driveDao.updateCorrection(
                driveId, startKm, endKm,
                startLat, startLng, startAddress,
                endLat, endLng, endAddress,
            )
        }
    }

    override suspend fun markKmPending(driveId: Long) {
        driveDao.markKmPending(driveId)
    }

    override suspend fun resolvePendingKm(driveId: Long, endKm: Int) {
        driveDao.resolvePendingKm(driveId, endKm)
    }

    override suspend fun updateKmMismatch(driveId: Long, hasKmMismatch: Boolean) {
        driveDao.updateKmMismatch(driveId, hasKmMismatch)
    }

    override suspend fun deleteDrive(driveId: Long) {
        driveDao.deleteById(driveId)
    }

    override suspend fun setProjectAndNote(driveId: Long, projectId: Long?, note: String?) {
        driveDao.updateProjectAndNote(driveId, projectId, note)
    }

    override suspend fun linkToProject(driveIds: List<Long>, projectId: Long) {
        if (driveIds.isNotEmpty()) driveDao.linkToProject(driveIds, projectId)
    }

    override suspend fun findByStartTime(startTime: Long): Drive? =
        driveDao.getByStartSecond(startTime / 1000)?.toDomain()

    override suspend fun insertDrive(drive: Drive): Long = driveDao.insert(
        DriveEntity(
            startTime = drive.startTime,
            endTime = drive.endTime,
            startLat = drive.startLat,
            startLng = drive.startLng,
            startAddress = drive.startAddress,
            endLat = drive.endLat,
            endLng = drive.endLng,
            endAddress = drive.endAddress,
            driveType = drive.driveType.name,
            startKm = drive.startKm,
            endKm = drive.endKm,
            hasKmMismatch = drive.hasKmMismatch,
            kmPending = drive.kmPending,
            projectId = drive.projectId,
            note = drive.note,
        )
    )

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
        hasKmMismatch = hasKmMismatch,
        kmPending = kmPending,
        projectId = projectId,
        note = note,
    )
}
