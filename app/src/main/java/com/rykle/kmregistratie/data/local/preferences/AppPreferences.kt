package com.rykle.kmregistratie.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "km_registratie_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var vin: String
        get() = prefs.getString(KEY_VIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VIN, value).apply()

    var vccApiKey: String
        get() = prefs.getString(KEY_VCC_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VCC_API_KEY, value).apply()

    var accessToken: String
        get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var clientId: String
        get() = prefs.getString(KEY_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CLIENT_ID, value).apply()

    var clientSecret: String
        get() = prefs.getString(KEY_CLIENT_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CLIENT_SECRET, value).apply()

    var refreshToken: String
        get() = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var pendingCodeVerifier: String
        get() = prefs.getString(KEY_PENDING_VERIFIER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PENDING_VERIFIER, value).apply()

    companion object {
        private const val KEY_VIN = "vin"
        private const val KEY_VCC_API_KEY = "vcc_api_key"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_CLIENT_SECRET = "client_secret"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_PENDING_VERIFIER = "pending_code_verifier"
    }
}
