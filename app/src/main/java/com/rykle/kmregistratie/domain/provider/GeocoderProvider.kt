package com.rykle.kmregistratie.domain.provider

import com.rykle.kmregistratie.domain.model.LocationPoint

interface GeocoderProvider {
    suspend fun getAddress(lat: Double, lng: Double): String?
    suspend fun getLocationFromAddress(address: String): LocationPoint?
}
