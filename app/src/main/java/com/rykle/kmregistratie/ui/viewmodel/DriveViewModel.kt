package com.rykle.kmregistratie.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.usecase.DeleteDriveUseCase
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import com.rykle.kmregistratie.domain.usecase.ExportDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.GetDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.GetPendingKmDriveUseCase
import com.rykle.kmregistratie.domain.usecase.ResolvePendingKmUseCase
import com.rykle.kmregistratie.domain.usecase.StartDriveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PendingKmUiState(
    val drive: Drive,
    val suggestedEndKm: Int?,
    val isSyncing: Boolean = false,
)

@HiltViewModel
class DriveViewModel @Inject constructor(
    getDrivesUseCase: GetDrivesUseCase,
    private val exportDrivesUseCase: ExportDrivesUseCase,
    private val startDriveUseCase: StartDriveUseCase,
    private val endDriveUseCase: EndDriveUseCase,
    private val deleteDriveUseCase: DeleteDriveUseCase,
    private val getPendingKmDriveUseCase: GetPendingKmDriveUseCase,
    private val resolvePendingKmUseCase: ResolvePendingKmUseCase,
    val prefs: AppPreferences
) : ViewModel() {

    val drives: StateFlow<List<Drive>> = getDrivesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Rit waarvan de eindstand nog ingevuld moet worden; niet-null = dialoog tonen.
    private val _pendingKm = MutableStateFlow<PendingKmUiState?>(null)
    val pendingKm: StateFlow<PendingKmUiState?> = _pendingKm.asStateFlow()

    init {
        // Bij het openen van de app direct vragen naar eindstanden die nog openstaan.
        loadPendingKm()
    }

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
        viewModelScope.launch {
            val kmPending = endDriveUseCase()
            if (kmPending) loadPendingKm()
        }
    }

    fun deleteDrive(driveId: Long) {
        viewModelScope.launch { deleteDriveUseCase(driveId) }
    }

    fun getDriveById(driveId: Long) = drives.value.firstOrNull { it.id == driveId }

    // ── Openstaande eindstand ─────────────────────────────────────────────────

    private fun loadPendingKm() {
        viewModelScope.launch {
            val pending = getPendingKmDriveUseCase()
            _pendingKm.value = pending?.let { PendingKmUiState(it.drive, it.suggestedEndKm) }
        }
    }

    fun syncPendingKm() {
        val current = _pendingKm.value ?: return
        _pendingKm.value = current.copy(isSyncing = true)
        loadPendingKm()
    }

    fun confirmPendingKm(endKm: Int) {
        val current = _pendingKm.value ?: return
        _pendingKm.value = null
        viewModelScope.launch {
            resolvePendingKmUseCase(current.drive.id, endKm)
            loadPendingKm()
        }
    }

    fun deletePendingKmDrive() {
        val current = _pendingKm.value ?: return
        _pendingKm.value = null
        viewModelScope.launch {
            deleteDriveUseCase(current.drive.id)
            loadPendingKm()
        }
    }

    /** Pas bij de volgende start van de app opnieuw vragen. */
    fun postponePendingKm() {
        _pendingKm.value = null
    }

    fun saveSettings(vin: String, apiKey: String, clientId: String, clientSecret: String) {
        prefs.vin = vin
        prefs.vccApiKey = apiKey
        prefs.clientId = clientId
        prefs.clientSecret = clientSecret
    }
}
