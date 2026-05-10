package com.rykle.kmregistratie.domain.repository

import com.rykle.kmregistratie.domain.model.SavedLocation

interface LocationHistoryRepository {
    fun addLocation(location: SavedLocation)
    fun getRecentLocations(): List<SavedLocation>
    fun getLastKm(): Int?
    fun saveLastKm(km: Int)
    fun getHomeLocation(): SavedLocation
}
