package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.repository.DriveRepository
import javax.inject.Inject

class ContinueDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository
) {
    suspend operator fun invoke() {
        val activeDrive = driveRepository.getActiveDrive()
        if (activeDrive == null) {
            val lastDrive = driveRepository.getLatestCompletedDrive()
            if (lastDrive != null) driveRepository.reopenDrive(lastDrive.id)
        }
    }
}
