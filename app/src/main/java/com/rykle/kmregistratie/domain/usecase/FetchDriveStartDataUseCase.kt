package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.provider.LocationProvider
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import javax.inject.Inject

data class DriveStartData(
    val location: LocationPoint?,
    val km: Int?,
    val lastKm: Int?,
)

class FetchDriveStartDataUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val volvoRepository: VolvoRepository,
    private val locationHistory: LocationHistoryRepository,
) {
    suspend operator fun invoke(): DriveStartData {
        val location = locationProvider.getCurrentLocation()
        val km = runCatching { volvoRepository.getCurrentOdometer() }.getOrNull()
        val lastKm = locationHistory.getLastKm()
        return DriveStartData(location, km, lastKm)
    }
}
