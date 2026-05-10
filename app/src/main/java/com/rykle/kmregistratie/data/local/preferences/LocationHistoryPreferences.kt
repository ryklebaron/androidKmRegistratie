package com.rykle.kmregistratie.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rykle.kmregistratie.domain.model.SavedLocation
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationHistoryPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationHistoryRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("location_history", Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    private val home = SavedLocation(
        address = "Korenlaan 18, Middelstum",
        latitude = 53.3408,
        longitude = 6.6292,
    )

    override fun getHomeLocation(): SavedLocation = home

    override fun addLocation(location: SavedLocation) {
        if (location.address.isBlank()) return
        val current = getRecentLocations().toMutableList()
        current.removeAll { it.address == location.address }
        current.add(0, location)
        prefs.edit().putString(KEY_LOCATIONS, gson.toJson(current.take(MAX_HISTORY))).apply()
    }

    override fun getRecentLocations(): List<SavedLocation> {
        val json = prefs.getString(KEY_LOCATIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            gson.fromJson<List<SavedLocation>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getLastKm(): Int? = prefs.getInt(KEY_LAST_KM, -1).takeIf { it >= 0 }

    override fun saveLastKm(km: Int) {
        prefs.edit().putInt(KEY_LAST_KM, km).apply()
    }

    companion object {
        private const val KEY_LOCATIONS = "recent_locations"
        private const val KEY_LAST_KM = "last_km"
        private const val MAX_HISTORY = 10
    }
}
