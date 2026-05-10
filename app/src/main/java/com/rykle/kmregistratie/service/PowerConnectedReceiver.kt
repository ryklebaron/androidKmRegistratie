package com.rykle.kmregistratie.service

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
        postPickerNotification(context, connectionTracker.canContinueDrive())
    }
}
