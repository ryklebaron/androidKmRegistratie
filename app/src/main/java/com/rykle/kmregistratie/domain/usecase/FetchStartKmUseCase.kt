package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.OdometerResult
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import javax.inject.Inject

data class StartKm(
    val odometer: OdometerResult,
    /** Voorstel als Volvo faalt: eindstand van de vorige rit, anders de laatst bekende stand. */
    val suggestedKm: Int?,
)

class FetchStartKmUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val volvoRepository: VolvoRepository,
    private val locationHistory: LocationHistoryRepository,
) {
    suspend operator fun invoke(): StartKm {
        val odometer = volvoRepository.fetchOdometer()
        val lastDrive = driveRepository.getLatestCompletedDrive()
        val suggestedKm = lastDrive?.endKm?.takeIf { !lastDrive.kmPending }
            ?: locationHistory.getLastKm()
        return StartKm(odometer, suggestedKm)
    }
}
