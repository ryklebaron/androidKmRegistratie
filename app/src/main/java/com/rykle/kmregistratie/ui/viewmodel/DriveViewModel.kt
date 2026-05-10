package com.rykle.kmregistratie.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.usecase.ContinueDriveUseCase
import com.rykle.kmregistratie.domain.usecase.DeleteDriveUseCase
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import com.rykle.kmregistratie.domain.usecase.ExportDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.GetDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.StartDriveUseCase
import com.rykle.kmregistratie.service.ConnectionTracker
import com.rykle.kmregistratie.service.postPickerNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DriveViewModel @Inject constructor(
    getDrivesUseCase: GetDrivesUseCase,
    private val exportDrivesUseCase: ExportDrivesUseCase,
    private val startDriveUseCase: StartDriveUseCase,
    private val endDriveUseCase: EndDriveUseCase,
    private val continueDriveUseCase: ContinueDriveUseCase,
    private val deleteDriveUseCase: DeleteDriveUseCase,
    private val connectionTracker: ConnectionTracker,
    val prefs: AppPreferences
) : ViewModel() {

    val drives: StateFlow<List<Drive>> = getDrivesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun exportAndShare(context: Context, from: Long? = null, to: Long? = null) {
        viewModelScope.launch {
            val csv = exportDrivesUseCase(from, to)
            val file = File(context.cacheDir, "exports/km_export.csv").also { f ->
                f.parentFile?.mkdirs()
                f.writeText(csv)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exporteer ritten"))
        }
    }

    fun startDrive(driveType: DriveType) {
        viewModelScope.launch { startDriveUseCase(driveType) }
    }

    fun endDrive() {
        viewModelScope.launch { endDriveUseCase() }
    }

    fun continueDrive() {
        viewModelScope.launch {
            continueDriveUseCase()
            connectionTracker.clearProvisionalEnd()
        }
    }

    // Alleen true als de AA-sessie binnen 60 seconden verbroken werd —
    // niet bij handmatig stoppen via de telefoon.
    fun canContinueDrive(): Boolean = connectionTracker.canContinueDrive()

    fun deleteDrive(driveId: Long) {
        viewModelScope.launch { deleteDriveUseCase(driveId) }
    }

    fun getDriveById(driveId: Long) = drives.value.firstOrNull { it.id == driveId }

    fun postTestPickerNotification(context: Context) {
        postPickerNotification(context, connectionTracker.canContinueDrive())
    }

    fun saveSettings(vin: String, apiKey: String, token: String) {
        prefs.vin = vin
        prefs.vccApiKey = apiKey
        prefs.accessToken = token
    }
}
