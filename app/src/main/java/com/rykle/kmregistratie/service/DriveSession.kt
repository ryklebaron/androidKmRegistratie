package com.rykle.kmregistratie.service

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import com.rykle.kmregistratie.domain.usecase.NextcloudSyncUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DriveSession(
    private val endDriveUseCase: EndDriveUseCase,
    private val connectionTracker: ConnectionTracker,
    private val nextcloudSyncUseCase: NextcloudSyncUseCase,
) : Session() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                connectionTracker.onDisconnected()
                scope.launch {
                    endDriveUseCase()
                    if (nextcloudSyncUseCase.isConfigured) nextcloudSyncUseCase()
                }
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen = DrivingScreen(carContext)
}
