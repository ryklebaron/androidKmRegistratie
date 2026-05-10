package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.repository.DriveRepository
import javax.inject.Inject

class DeleteDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository
) {
    suspend operator fun invoke(driveId: Long) = driveRepository.deleteDrive(driveId)
}
