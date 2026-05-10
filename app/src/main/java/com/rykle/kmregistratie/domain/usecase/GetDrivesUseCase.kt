package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.repository.DriveRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDrivesUseCase @Inject constructor(
    private val driveRepository: DriveRepository
) {
    operator fun invoke(): Flow<List<Drive>> = driveRepository.getAllDrives()
}
