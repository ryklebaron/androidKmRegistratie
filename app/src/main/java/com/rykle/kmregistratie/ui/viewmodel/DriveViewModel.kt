package com.rykle.kmregistratie.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.usecase.ContinueDriveUseCase
import com.rykle.kmregistratie.domain.usecase.DeleteDriveUseCase
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import com.rykle.kmregistratie.domain.usecase.ExportDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.GetDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.ImportDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.NextcloudSyncUseCase
import com.rykle.kmregistratie.domain.usecase.StartDriveUseCase
import com.rykle.kmregistratie.service.ConnectionTracker
import com.rykle.kmregistratie.service.postPickerNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val timestamp: Long) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@HiltViewModel
class DriveViewModel @Inject constructor(
    getDrivesUseCase: GetDrivesUseCase,
    private val exportDrivesUseCase: ExportDrivesUseCase,
    private val importDrivesUseCase: ImportDrivesUseCase,
    private val startDriveUseCase: StartDriveUseCase,
    private val endDriveUseCase: EndDriveUseCase,
    private val continueDriveUseCase: ContinueDriveUseCase,
    private val deleteDriveUseCase: DeleteDriveUseCase,
    private val nextcloudSyncUseCase: NextcloudSyncUseCase,
    private val connectionTracker: ConnectionTracker,
    private val driveRepository: DriveRepository,
    val prefs: AppPreferences
) : ViewModel() {

    val drives: StateFlow<List<Drive>> = getDrivesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

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
            endDriveUseCase()
            if (nextcloudSyncUseCase.isConfigured) performSync()
        }
    }

    fun syncToNextcloud() {
        viewModelScope.launch { performSync() }
    }

    private suspend fun performSync() {
        _syncStatus.value = SyncStatus.Syncing
        nextcloudSyncUseCase()
            .onSuccess { _syncStatus.value = SyncStatus.Success(System.currentTimeMillis()) }
            .onFailure { _syncStatus.value = SyncStatus.Error(it.message ?: "Onbekende fout") }
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

    fun deleteMultipleDrives(ids: Set<Long>) {
        viewModelScope.launch { ids.forEach { deleteDriveUseCase(it) } }
    }

    fun exportAndShareSelected(context: Context, ids: Set<Long>) {
        viewModelScope.launch {
            val csv = exportDrivesUseCase(ids = ids)
            val file = File(context.cacheDir, "exports/km_export.csv").also { f ->
                f.parentFile?.mkdirs()
                f.writeText(csv)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exporteer ritten"))
        }
    }

    val nextcloudConfigured: Boolean get() = nextcloudSyncUseCase.isConfigured

    fun getDriveById(driveId: Long) = drives.value.firstOrNull { it.id == driveId }

    fun postTestPickerNotification(context: Context) {
        postPickerNotification(context, connectionTracker.canContinueDrive())
    }

    fun saveSettings(vin: String, apiKey: String, clientId: String, clientSecret: String) {
        prefs.vin = vin
        prefs.vccApiKey = apiKey
        prefs.clientId = clientId
        prefs.clientSecret = clientSecret
    }

    fun importFromNextcloud(onResult: (imported: Int, skipped: Int, error: Boolean) -> Unit) {
        viewModelScope.launch {
            nextcloudSyncUseCase.download()
                .onFailure { onResult(0, 0, true) }
                .onSuccess { csv ->
                    driveRepository.deleteAllDrives()
                    importDrivesUseCase(csv)
                        .onSuccess { onResult(it.imported, it.skipped, false) }
                        .onFailure { onResult(0, 0, true) }
                }
        }
    }

    fun importDrives(context: android.content.Context, uri: android.net.Uri, onResult: (imported: Int, skipped: Int, error: Boolean) -> Unit) {
        viewModelScope.launch {
            val csv = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                }.getOrNull()
            }
            if (csv == null) { onResult(0, 0, true); return@launch }
            importDrivesUseCase(csv)
                .onSuccess { onResult(it.imported, it.skipped, false) }
                .onFailure { onResult(0, 0, true) }
        }
    }

    fun saveNextcloudSettings(url: String, username: String, password: String) {
        prefs.nextcloudUrl = url
        prefs.nextcloudUsername = username
        prefs.nextcloudPassword = password
    }

}
