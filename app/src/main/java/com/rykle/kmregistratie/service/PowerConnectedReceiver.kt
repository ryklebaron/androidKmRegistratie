package com.rykle.kmregistratie.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PowerConnectedReceiver : BroadcastReceiver() {

    @Inject lateinit var connectionTracker: ConnectionTracker

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        if (!keyguard.isKeyguardLocked) return
        postPickerNotification(context, connectionTracker.canContinueDrive())
    }
}
