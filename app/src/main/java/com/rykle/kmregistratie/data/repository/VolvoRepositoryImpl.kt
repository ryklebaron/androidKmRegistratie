package com.rykle.kmregistratie.data.repository

import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.data.remote.VolvoApiService
import com.rykle.kmregistratie.data.remote.VolvoAuthService
import com.rykle.kmregistratie.data.remote.auth.VolvoPkce
import com.rykle.kmregistratie.domain.model.OdometerResult
import com.rykle.kmregistratie.domain.model.VolvoStatus
import com.rykle.kmregistratie.domain.repository.VolvoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class VolvoRepositoryImpl @Inject constructor(
    private val apiService: VolvoApiService,
    private val authService: VolvoAuthService,
    private val prefs: AppPreferences,
) : VolvoRepository {

    private val _status = MutableStateFlow<VolvoStatus>(VolvoStatus.Unknown)
    override val status: StateFlow<VolvoStatus> = _status.asStateFlow()

    override suspend fun fetchOdometer(): OdometerResult {
        _status.value = VolvoStatus.Checking
        val result = requestOdometer()
        val now = System.currentTimeMillis()
        _status.value = when (result) {
            is OdometerResult.Success -> VolvoStatus.Connected(result.km, now)
            is OdometerResult.Failure -> VolvoStatus.Failed(result.reason, now)
        }
        return result
    }

    private suspend fun requestOdometer(): OdometerResult {
        val vin = prefs.vin.takeIf { it.isNotBlank() }
            ?: return OdometerResult.Failure("Geen VIN ingesteld (zie Instellingen)")
        val apiKey = prefs.vccApiKey.takeIf { it.isNotBlank() }
            ?: return OdometerResult.Failure("Geen VCC API-key ingesteld (zie Instellingen)")
        val token = getValidToken()
            ?: return OdometerResult.Failure("Niet ingelogd bij Volvo (zie Instellingen)")

        return try {
            odometerOf(apiService.getOdometer(vin, "Bearer $token", apiKey).data?.odometer?.value)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val refreshed = refreshAccessToken()
                    ?: return OdometerResult.Failure("Volvo-sessie verlopen, log opnieuw in (zie Instellingen)")
                try {
                    odometerOf(apiService.getOdometer(vin, "Bearer $refreshed", apiKey).data?.odometer?.value)
                } catch (e: Exception) {
                    failureOf(e)
                }
            } else failureOf(e)
        } catch (e: Exception) {
            failureOf(e)
        }
    }

    private fun odometerOf(value: Int?): OdometerResult =
        value?.let { OdometerResult.Success(it) }
            ?: OdometerResult.Failure("Volvo gaf geen km-stand terug")

    private fun failureOf(e: Exception): OdometerResult.Failure = OdometerResult.Failure(
        when (e) {
            is HttpException -> when (e.code()) {
                401, 403 -> "Geen toegang tot de Volvo API (fout ${e.code()}), log opnieuw in"
                404 -> "Auto niet gevonden bij Volvo, controleer het VIN"
                429 -> "Te veel verzoeken naar Volvo, probeer het straks opnieuw"
                else -> "Volvo API gaf fout ${e.code()}"
            }
            is IOException -> "Geen internet of Volvo niet bereikbaar"
            else -> "Onverwachte fout: ${e.message ?: e.javaClass.simpleName}"
        }
    )

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
