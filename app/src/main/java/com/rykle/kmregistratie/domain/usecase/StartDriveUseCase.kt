package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.model.SavedLocation
import com.rykle.kmregistratie.domain.provider.GeocoderProvider
import com.rykle.kmregistratie.domain.provider.LocationProvider
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import javax.inject.Inject

class StartDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val volvoRepository: VolvoRepository,
    private val locationProvider: LocationProvider,
    private val geocoderProvider: GeocoderProvider,
    private val locationHistory: LocationHistoryRepository,
) {
    // Auto-fetch path: called from notification buttons or other non-interactive contexts.
    suspend operator fun invoke(driveType: DriveType) {
        val gps = locationProvider.getCurrentLocation()
        val (loc, knownAddress) = if (gps != null) {
            Pair(gps, null)
        } else {
            val last = locationHistory.getRecentLocations().firstOrNull()
            Pair(last?.let { LocationPoint(it.latitude, it.longitude) }, last?.address)
        }
        val km = runCatching { volvoRepository.getCurrentOdometer() }.getOrNull()
            ?: locationHistory.getLastKm()
        execute(driveType, loc, knownAddress, km)
    }

    // Manual path: called from Android Auto after the user has selected or confirmed values.
    suspend operator fun invoke(
        driveType: DriveType,
        location: LocationPoint?,
        address: String?,
        km: Int?,
    ) {
        execute(driveType, location, address, km)
    }

    private suspend fun execute(
        driveType: DriveType,
        location: LocationPoint?,
        address: String?,
        km: Int?,
    ) {
        val activeDrive = driveRepository.getActiveDrive()
        if (activeDrive != null) {
            driveRepository.endDrive(
                driveId = activeDrive.id,
                endTime = System.currentTimeMillis(),
                endLat = 0.0,
                endLng = 0.0,
                endAddress = null,
                endKm = null,
            )
        }

        val lastDrive = driveRepository.getLatestCompletedDrive()
        val hasKmMismatch = km != null && lastDrive?.endKm != null && km != lastDrive.endKm

        val driveId = driveRepository.startDrive(
            startTime = System.currentTimeMillis(),
            startLat = location?.latitude ?: 0.0,
            startLng = location?.longitude ?: 0.0,
            startAddress = address,
            driveType = driveType,
            startKm = km,
            hasKmMismatch = hasKmMismatch,
        )

        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        if (lat != 0.0 || lng != 0.0) {
            val finalAddress = address ?: geocoderProvider.getAddress(lat, lng)?.also { a ->
                driveRepository.updateStartAddress(driveId, a)
            }
            if (finalAddress != null) {
                locationHistory.addLocation(SavedLocation(finalAddress, lat, lng))
            }
        }

        if (km != null) locationHistory.saveLastKm(km)
    }
}
