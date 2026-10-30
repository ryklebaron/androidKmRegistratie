package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.DriveCorrection
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import javax.inject.Inject

class CorrectDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val locationHistory: LocationHistoryRepository,
) {
    suspend operator fun invoke(driveId: Long, correction: DriveCorrection) {
        val drive = driveRepository.getDriveById(driveId) ?: return
        val applied = if (drive.isActive) {
            correction.copy(endKm = null, endLat = null, endLng = null, endAddress = null)
        } else correction
        driveRepository.applyCorrection(driveId, applied)

        val previous = driveRepository.getPreviousDrive(drive)
        val mismatch = applied.startKm != null && previous?.endKm != null &&
            !previous.kmPending && applied.startKm != previous.endKm
        driveRepository.updateKmMismatch(driveId, mismatch)

        val next = driveRepository.getNextDrive(drive)
        if (next != null) {
            if (next.startKm != null && applied.endKm != null) {
                driveRepository.updateKmMismatch(next.id, next.startKm != applied.endKm)
            }
        } else {
            (applied.endKm ?: applied.startKm)?.let { locationHistory.saveLastKm(it) }
        }
    }

    suspend fun linkToProject(driveIds: List<Long>, projectId: Long) {
        driveRepository.linkToProject(driveIds, projectId)
    }

    suspend fun setProjectAndNote(driveId: Long, projectId: Long?, note: String?) {
        driveRepository.setProjectAndNote(driveId, projectId, note?.trim()?.ifBlank { null })
    }
}
