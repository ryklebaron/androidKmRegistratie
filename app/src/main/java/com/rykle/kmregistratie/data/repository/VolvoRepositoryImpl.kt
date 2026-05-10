package com.rykle.kmregistratie.data.repository

import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.data.remote.VolvoApiService
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import javax.inject.Inject

class VolvoRepositoryImpl @Inject constructor(
    private val apiService: VolvoApiService,
    private val prefs: AppPreferences
) : VolvoRepository {

    override suspend fun getCurrentOdometer(): Int? {
        val vin = prefs.vin.takeIf { it.isNotBlank() } ?: return null
        val token = prefs.accessToken.takeIf { it.isNotBlank() } ?: return null
        val apiKey = prefs.vccApiKey.takeIf { it.isNotBlank() } ?: return null

        return apiService
            .getOdometer(vin, "Bearer $token", apiKey)
            .data?.odometer?.value
    }
}
