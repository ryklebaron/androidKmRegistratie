package com.rykle.kmregistratie.domain.usecase

import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class NextcloudSyncUseCase @Inject constructor(
    private val exportDrivesUseCase: ExportDrivesUseCase,
    private val prefs: AppPreferences,
    private val client: OkHttpClient,
) {
    val isConfigured: Boolean
        get() = prefs.nextcloudUrl.isNotEmpty() &&
                prefs.nextcloudUsername.isNotEmpty() &&
                prefs.nextcloudPassword.isNotEmpty()

    suspend fun download(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            check(isConfigured) { "Nextcloud niet geconfigureerd" }
            val url = prefs.nextcloudUrl.trimEnd('/')
            val username = prefs.nextcloudUsername
            val password = prefs.nextcloudPassword
            val request = Request.Builder()
                .url("$url/remote.php/dav/files/$username/km_ritten.csv")
                .header("Authorization", Credentials.basic(username, password))
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}: ${response.message}" }
                response.body?.string() ?: error("Leeg bestand ontvangen")
            }
        }
    }

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(isConfigured) { "Nextcloud niet geconfigureerd" }
            val url = prefs.nextcloudUrl.trimEnd('/')
            val username = prefs.nextcloudUsername
            val password = prefs.nextcloudPassword
            val csv = exportDrivesUseCase()
            val webdavUrl = "$url/remote.php/dav/files/$username/km_ritten.csv"
            val request = Request.Builder()
                .url(webdavUrl)
                .header("Authorization", Credentials.basic(username, password))
                .put(csv.toRequestBody("text/csv".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}: ${response.message}" }
            }
        }
    }
}
