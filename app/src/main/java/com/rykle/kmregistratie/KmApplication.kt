package com.rykle.kmregistratie

import android.app.Application
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.car.app.connection.CarConnection
import com.rykle.kmregistratie.service.ConnectionTracker
import com.rykle.kmregistratie.service.postPickerNotification
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KmApplication : Application() {

    @Inject lateinit var connectionTracker: ConnectionTracker

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        postPickerIfLocked()
        observeCarConnection()
    }

    private fun postPickerIfLocked() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard.isKeyguardLocked) {
            postPickerNotification(this, connectionTracker.canContinueDrive())
        }
    }

    private fun observeCarConnection() {
        CarConnection(this).type.observeForever { connectionType ->
            if (connectionType == CarConnection.CONNECTION_TYPE_PROJECTION) {
                postPickerNotification(this, connectionTracker.canContinueDrive())
            }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DRIVE_ACTIVE,
                "Actieve rit",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Toont de actieve rit in Android Auto" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DRIVE_PICKER,
                "Rit starten",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Kies het type rit bij verbinding met Android Auto" }
        )
    }

    companion object {
        const val CHANNEL_DRIVE_ACTIVE = "drive_active"
        const val NOTIF_DRIVE_ACTIVE = 1
        const val CHANNEL_DRIVE_PICKER = "drive_picker"
        const val NOTIF_DRIVE_PICKER = 2
    }
}
