package com.rykle.kmregistratie.domain.provider

import com.rykle.kmregistratie.domain.model.LocationPoint

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationPoint?
}
