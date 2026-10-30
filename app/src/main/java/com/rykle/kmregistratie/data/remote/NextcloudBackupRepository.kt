package com.rykle.kmregistratie.data.remote

import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.domain.repository.CloudBackupRepository
import com.rykle.kmregistratie.domain.repository.CloudBackupRepository.Companion.MAIN_FILE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/** Zet de CSV's in de hoofdmap van de Nextcloud-gebruiker (WebDAV). */
class NextcloudBackupRepository @Inject constructor(
    private val prefs: AppPreferences,
    private val client: OkHttpClient,
) : CloudBackupRepository {

    override val isConfigured: Boolean
        get() = prefs.nextcloudUrl.isNotBlank() &&
            prefs.nextcloudUsername.isNotBlank() &&
            prefs.nextcloudPassword.isNotBlank()

    override suspend fun upload(csv: String, fileName: String) = withContext(Dispatchers.IO) {
        execute(request(fileName).put(csv.toRequestBody("text/csv; charset=utf-8".toMediaType()))) { }
            ?: error("Nextcloud-map niet gevonden (controleer de URL)")
    }

    override suspend fun download(): String? = withContext(Dispatchers.IO) {
        execute(request(MAIN_FILE).get()) { body -> body }
    }

    private fun request(fileName: String): Request.Builder {
        check(isConfigured) { "Nextcloud is nog niet ingesteld" }
        val base = baseUrl(prefs.nextcloudUrl)
        val user = prefs.nextcloudUsername.trim()
        return Request.Builder()
            .url("$base/remote.php/dav/files/$user/$fileName")
            .header("Authorization", Credentials.basic(user, prefs.nextcloudPassword))
    }

    /**
     * Basisadres van de server. Werkt ook als de gebruiker het WebDAV-adres uit Nextcloud plakt
     * (…/remote.php/dav/files/<gebruiker>/ of …/remote.php/webdav/), of zonder https:// typt.
     */
    private fun baseUrl(input: String): String {
        val url = input.trim().substringBefore("/remote.php").trimEnd('/')
        return if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    }

    /** null bij 404; andere fouten worden een leesbare melding. */
    private fun <T> execute(builder: Request.Builder, onBody: (String) -> T): T? =
        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 404) return@use null
            check(response.isSuccessful) {
                if (response.code == 401) "Nextcloud weigert de inlog (controleer gebruikersnaam en app-wachtwoord)"
                else "Nextcloud gaf HTTP ${response.code}"
            }
            onBody(response.body?.string().orEmpty())
        }
}
