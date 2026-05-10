package com.rykle.kmregistratie.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import com.rykle.kmregistratie.KmApplication
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.usecase.ContinueDriveUseCase
import com.rykle.kmregistratie.domain.usecase.StartDriveUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DriveActionReceiver : BroadcastReceiver() {

    @Inject lateinit var startDriveUseCase: StartDriveUseCase
    @Inject lateinit var continueDriveUseCase: ContinueDriveUseCase
    @Inject lateinit var connectionTracker: ConnectionTracker

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                when (intent.action) {
                    ACTION_BUSINESS -> startDriveUseCase(DriveType.BUSINESS)
                    ACTION_PRIVATE  -> startDriveUseCase(DriveType.PRIVATE)
                    ACTION_CONTINUE -> {
                        continueDriveUseCase()
                        connectionTracker.clearProvisionalEnd()
                    }
                }
                NotificationManagerCompat.from(context).cancel(KmApplication.NOTIF_DRIVE_PICKER)
                vibrate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun vibrate(context: Context) {
        val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(effect)
        }
    }

    companion object {
        const val ACTION_BUSINESS = "com.rykle.kmregistratie.ACTION_BUSINESS"
        const val ACTION_PRIVATE  = "com.rykle.kmregistratie.ACTION_PRIVATE"
        const val ACTION_CONTINUE = "com.rykle.kmregistratie.ACTION_CONTINUE"
    }
}
