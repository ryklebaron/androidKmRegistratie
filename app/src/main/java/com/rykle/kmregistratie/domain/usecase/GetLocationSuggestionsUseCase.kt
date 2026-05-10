package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import javax.inject.Inject

data class LocationSuggestion(
    val label: String,
    val address: String,
    val location: LocationPoint,
)

class GetLocationSuggestionsUseCase @Inject constructor(
    private val locationHistory: LocationHistoryRepository,
) {
    operator fun invoke(): List<LocationSuggestion> {
        val suggestions = mutableListOf<LocationSuggestion>()
        val seen = mutableSetOf<String>()

        locationHistory.getRecentLocations().forEach { saved ->
            if (seen.add(saved.address)) {
                suggestions.add(
                    LocationSuggestion(
                        label = saved.address,
                        address = saved.address,
                        location = LocationPoint(saved.latitude, saved.longitude),
                    )
                )
            }
        }

        val home = locationHistory.getHomeLocation()
        if (seen.add(home.address)) {
            suggestions.add(
                LocationSuggestion(
                    label = "Thuis: ${home.address}",
                    address = home.address,
                    location = LocationPoint(home.latitude, home.longitude),
                )
            )
        }

        return suggestions
    }
}
