package com.rykle.kmregistratie.data.remote.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object VolvoPkce {
    fun basicAuthHeader(clientId: String, clientSecret: String): String {
        val credentials = Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
        return "Basic $credentials"
    }

    fun generateVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
