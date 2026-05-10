package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.model.SavedLocation
import com.rykle.kmregistratie.domain.provider.GeocoderProvider
import com.rykle.kmregistratie.domain.provider.LocationProvider
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import javax.inject.Inject

class EndDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val volvoRepository: VolvoRepository,
    private val locationProvider: LocationProvider,
    private val geocoderProvider: GeocoderProvider,
    private val locationHistory: LocationHistoryRepository,
) {
    suspend operator fun invoke(manualEndKm: Int? = null) {
        val activeDrive = driveRepository.getActiveDrive() ?: return

        val gps = locationProvider.getCurrentLocation()
        // Fall back to last known location if GPS unavailable (session is ending, can't prompt user)
        val location: LocationPoint? = gps ?: run {
            val last = locationHistory.getRecentLocations().firstOrNull()
            last?.let { LocationPoint(it.latitude, it.longitude) }
        }

        val endKm = manualEndKm
            ?: runCatching { volvoRepository.getCurrentOdometer() }.getOrNull()
            ?: locationHistory.getLastKm()

        driveRepository.endDrive(
            driveId = activeDrive.id,
            endTime = System.currentTimeMillis(),
            endLat = location?.latitude ?: 0.0,
            endLng = location?.longitude ?: 0.0,
            endAddress = null,
            endKm = endKm,
        )

        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        if (lat != 0.0 || lng != 0.0) {
            val address = geocoderProvider.getAddress(lat, lng)
            if (address != null) {
                driveRepository.updateEndAddress(activeDrive.id, address)
                // Only add real GPS locations to history, not fallback locations already in history
                if (gps != null) locationHistory.addLocation(SavedLocation(address, lat, lng))
            }
        }

        if (endKm != null) locationHistory.saveLastKm(endKm)
    }
}
