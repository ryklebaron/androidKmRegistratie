package com.rykle.kmregistratie.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("connection_tracker", Context.MODE_PRIVATE)

    fun storeProvisionalEnd(disconnectTime: Long, lat: Double?, lng: Double?) {
        prefs.edit()
            .putLong(KEY_DISCONNECT_TIME, disconnectTime)
            .also { editor ->
                if (lat != null && lng != null) {
                    editor.putString(KEY_END_LAT, lat.toString())
                    editor.putString(KEY_END_LNG, lng.toString())
                }
            }
            .apply()
    }

    fun onDisconnected() {
        prefs.edit().putLong(KEY_DISCONNECT_TIME, System.currentTimeMillis()).apply()
    }

    fun canContinueDrive(): Boolean {
        val disconnectTime = prefs.getLong(KEY_DISCONNECT_TIME, 0L)
        if (disconnectTime == 0L) return false
        return System.currentTimeMillis() - disconnectTime < 60_000L
    }

    fun getDisconnectTime(): Long = prefs.getLong(KEY_DISCONNECT_TIME, 0L)

    fun getProvisionalEnd(): Pair<Double, Double>? {
        val lat = prefs.getString(KEY_END_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_END_LNG, null)?.toDoubleOrNull() ?: return null
        return lat to lng
    }

    fun clearProvisionalEnd() {
        prefs.edit()
            .remove(KEY_DISCONNECT_TIME)
            .remove(KEY_END_LAT)
            .remove(KEY_END_LNG)
            .apply()
    }

    companion object {
        private const val KEY_DISCONNECT_TIME = "disconnect_time"
        private const val KEY_END_LAT = "end_lat"
        private const val KEY_END_LNG = "end_lng"
    }
}
