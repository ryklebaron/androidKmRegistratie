package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import javax.inject.Inject

data class PendingKmDrive(
    val drive: Drive,
    /** Voorgestelde eindstand, of null als er nog geen nieuwe stand bekend is. */
    val suggestedEndKm: Int?,
)

class GetPendingKmDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val volvoRepository: VolvoRepository,
) {
    suspend operator fun invoke(): PendingKmDrive? {
        val drive = driveRepository.getOldestKmPendingDrive() ?: return null
        return PendingKmDrive(drive, suggestEndKm(drive))
    }

    private suspend fun suggestEndKm(drive: Drive): Int? {
        // Is er inmiddels een volgende rit gestart, dan is diens startstand de beste schatting;
        // de huidige Volvo-stand ligt dan al voorbij het einde van deze rit.
        val next = driveRepository.getNextDrive(drive)
        if (next != null) return next.startKm?.takeIf { it != drive.startKm }

        val odometer = runCatching { volvoRepository.getCurrentOdometer() }.getOrNull()
        return odometer?.takeIf { it != drive.startKm }
    }
}
