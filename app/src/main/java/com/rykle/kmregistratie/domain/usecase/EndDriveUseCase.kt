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
    /** Beëindigt de actieve rit en geeft true terug als de eindstand nog ingevuld moet worden. */
    suspend operator fun invoke(): Boolean {
        val activeDrive = driveRepository.getActiveDrive() ?: return false

        val gps = locationProvider.getCurrentLocation()
        // Fall back to last known location if GPS unavailable
        val location: LocationPoint? = gps ?: run {
            val last = locationHistory.getRecentLocations().firstOrNull()
            last?.let { LocationPoint(it.latitude, it.longitude) }
        }

        val endKm = runCatching { volvoRepository.getCurrentOdometer() }.getOrNull()
        // De auto stuurt pas na een tijdje vergrendeld staan een nieuwe stand naar de Volvo-cloud.
        // Gelijk aan de startstand (of geen stand) betekent dus: nog niet gesynct.
        val kmPending = endKm == null || endKm == activeDrive.startKm

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

        if (kmPending) {
            driveRepository.markKmPending(activeDrive.id)
        } else {
            locationHistory.saveLastKm(endKm)
        }
        return kmPending
    }
}
