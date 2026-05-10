package com.rykle.kmregistratie.service

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KmCarAppService : CarAppService() {

    @Inject lateinit var endDriveUseCase: EndDriveUseCase
    @Inject lateinit var connectionTracker: ConnectionTracker

    override fun onCreate() {
        super.onCreate()
        // Toon picker-notificatie zodra Android Auto verbinding maakt.
        // VISIBILITY_PUBLIC zorgt dat de knoppen bedienbaar zijn zonder te ontgrendelen.
        postPickerNotification(this, connectionTracker.canContinueDrive())
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPickerNotification(this)
    }

    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = DriveSession(endDriveUseCase, connectionTracker)
}
