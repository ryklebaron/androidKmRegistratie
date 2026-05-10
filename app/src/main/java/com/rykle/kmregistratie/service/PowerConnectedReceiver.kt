package com.rykle.kmregistratie.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PowerConnectedReceiver : BroadcastReceiver() {

    @Inject lateinit var connectionTracker: ConnectionTracker

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PowerConnectedReceiver", "onReceive: action=${intent.action}")
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return
        Log.d("PowerConnectedReceiver", "USB connected — posting picker notification")
        postPickerNotification(context, connectionTracker.canContinueDrive())
    }
}
