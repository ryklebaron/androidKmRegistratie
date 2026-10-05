package com.rykle.kmregistratie.data.repository

import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.data.remote.VolvoApiService
import com.rykle.kmregistratie.data.remote.VolvoAuthService
import com.rykle.kmregistratie.data.remote.auth.VolvoPkce
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import retrofit2.HttpException
import javax.inject.Inject

class VolvoRepositoryImpl @Inject constructor(
    private val apiService: VolvoApiService,
    private val authService: VolvoAuthService,
    private val prefs: AppPreferences,
) : VolvoRepository {

    override suspend fun getCurrentOdometer(): Int? {
        val vin = prefs.vin.takeIf { it.isNotBlank() } ?: return null
        val apiKey = prefs.vccApiKey.takeIf { it.isNotBlank() } ?: return null
        val token = getValidToken() ?: return null

        return try {
            apiService.getOdometer(vin, "Bearer $token", apiKey).data?.odometer?.value
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val refreshed = refreshAccessToken() ?: return null
                runCatching {
                    apiService.getOdometer(vin, "Bearer $refreshed", apiKey).data?.odometer?.value
                }.getOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getValidToken(): String? {
        val stored = prefs.accessToken.takeIf { it.isNotBlank() }
        return stored ?: refreshAccessToken()
    }

    private suspend fun refreshAccessToken(): String? {
        val clientId = prefs.clientId.takeIf { it.isNotBlank() } ?: return null
        val clientSecret = prefs.clientSecret.takeIf { it.isNotBlank() } ?: return null
        val refreshToken = prefs.refreshToken.takeIf { it.isNotBlank() } ?: return null

        return try {
            val response = authService.refreshToken(
                authorization = VolvoPkce.basicAuthHeader(clientId, clientSecret),
                grantType = "refresh_token",
                refreshToken = refreshToken,
                clientId = clientId,
            )
            prefs.accessToken = response.accessToken
            if (!response.refreshToken.isNullOrBlank()) {
                prefs.refreshToken = response.refreshToken
            }
            response.accessToken
        } catch (e: Exception) {
            null
        }
    }
}
