package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.model.SavedLocation
import com.rykle.kmregistratie.domain.provider.GeocoderProvider
import com.rykle.kmregistratie.domain.provider.LocationProvider
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import javax.inject.Inject

class StartDriveUseCase @Inject constructor(
    private val driveRepository: DriveRepository,
    private val locationProvider: LocationProvider,
    private val geocoderProvider: GeocoderProvider,
    private val locationHistory: LocationHistoryRepository,
) {
    /** [km] komt van de Volvo API, of is door de gebruiker bevestigd (zie [FetchStartKmUseCase]). */
    suspend operator fun invoke(driveType: DriveType, km: Int?, projectId: Long? = null) {
        val gps = locationProvider.getCurrentLocation()
        val (location, knownAddress) = if (gps != null) {
            Pair(gps, null)
        } else {
            val last = locationHistory.getRecentLocations().firstOrNull()
            Pair(last?.let { LocationPoint(it.latitude, it.longitude) }, last?.address)
        }

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
        // De startstand van deze rit is per definitie de eindstand van de vorige rit. Stond die
        // nog open (cloud nog niet gesynct bij het beëindigen), dan vullen we hem hier in.
        val resolvesPending = km != null && lastDrive != null && lastDrive.kmPending &&
            km != lastDrive.startKm
        if (resolvesPending && km != null && lastDrive != null) {
            driveRepository.resolvePendingKm(lastDrive.id, km)
        }

        // Bij een vorige rit die nog steeds geen eindstand heeft, wordt de mismatch pas bepaald
        // zodra die eindstand is ingevuld (zie ResolvePendingKmUseCase).
        val hasKmMismatch = !resolvesPending && km != null && lastDrive?.endKm != null &&
            !lastDrive.kmPending && km != lastDrive.endKm

        val driveId = driveRepository.startDrive(
            startTime = System.currentTimeMillis(),
            startLat = location?.latitude ?: 0.0,
            startLng = location?.longitude ?: 0.0,
            startAddress = knownAddress,
            driveType = driveType,
            startKm = km,
            hasKmMismatch = hasKmMismatch,
            projectId = projectId,
        )

        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        if (lat != 0.0 || lng != 0.0) {
            val finalAddress = knownAddress ?: geocoderProvider.getAddress(lat, lng)?.also { a ->
                driveRepository.updateStartAddress(driveId, a)
            }
            if (finalAddress != null) {
                locationHistory.addLocation(SavedLocation(finalAddress, lat, lng))
            }
        }

        if (km != null) locationHistory.saveLastKm(km)
    }
}
