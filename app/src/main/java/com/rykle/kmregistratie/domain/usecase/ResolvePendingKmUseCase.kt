package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import javax.inject.Inject

class ResolvePendingKmUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val locationHistory: LocationHistoryRepository,
) {
    suspend operator fun invoke(driveId: Long, endKm: Int) {
        val drive = driveRepository.getDriveById(driveId) ?: return
        driveRepository.resolvePendingKm(driveId, endKm)

        val next = driveRepository.getNextDrive(drive)
        if (next == null) {
            locationHistory.saveLastKm(endKm)
        } else if (next.startKm != null) {
            driveRepository.updateKmMismatch(next.id, next.startKm != endKm)
        }
    }
}
