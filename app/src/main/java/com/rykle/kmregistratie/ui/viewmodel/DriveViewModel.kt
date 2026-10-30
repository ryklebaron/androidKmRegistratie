package com.rykle.kmregistratie.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveCorrection
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.model.Project
import com.rykle.kmregistratie.domain.model.OdometerResult
import com.rykle.kmregistratie.domain.model.VolvoStatus
import com.rykle.kmregistratie.domain.usecase.DeleteDriveUseCase
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import com.rykle.kmregistratie.domain.usecase.ExportDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.FetchStartKmUseCase
import com.rykle.kmregistratie.domain.usecase.GetDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.GetPendingKmDriveUseCase
import com.rykle.kmregistratie.domain.usecase.ImportDrivesUseCase
import com.rykle.kmregistratie.domain.usecase.NextcloudSyncUseCase
import com.rykle.kmregistratie.domain.model.SavedLocation
import com.rykle.kmregistratie.domain.provider.GeocoderProvider
import com.rykle.kmregistratie.domain.provider.LocationProvider
import com.rykle.kmregistratie.domain.repository.LocationHistoryRepository
import com.rykle.kmregistratie.domain.repository.ProjectRepository
import com.rykle.kmregistratie.domain.repository.SuggestionRepository
import com.rykle.kmregistratie.domain.repository.SuggestionRepository.Rejection
import com.rykle.kmregistratie.domain.usecase.ResolvePendingKmUseCase
import com.rykle.kmregistratie.domain.usecase.StartDriveUseCase
import com.rykle.kmregistratie.domain.usecase.CorrectDriveUseCase
import com.rykle.kmregistratie.domain.usecase.VolvoConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PendingKmUiState(
    val drive: Drive,
    val suggestedEndKm: Int?,
    val isSyncing: Boolean = false,
)

/** Rit die gestart wordt; niet-null = startdialoog tonen. */
sealed interface StartDriveUiState {
    val driveType: DriveType

    data class FetchingKm(override val driveType: DriveType) : StartDriveUiState

    /** Volvo gaf geen stand; de gebruiker bevestigt [suggestedKm] of past hem aan. */
    data class VolvoFailed(
        override val driveType: DriveType,
        val reason: String,
        val suggestedKm: Int?,
        val isRetrying: Boolean = false,
    ) : StartDriveUiState
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Done(val atMillis: Long) : SyncStatus
    data class Failed(val reason: String) : SyncStatus
}

@HiltViewModel
class DriveViewModel @Inject constructor(
    getDrivesUseCase: GetDrivesUseCase,
    private val exportDrivesUseCase: ExportDrivesUseCase,
    private val startDriveUseCase: StartDriveUseCase,
    private val fetchStartKmUseCase: FetchStartKmUseCase,
    private val volvoConnectionUseCase: VolvoConnectionUseCase,
    private val endDriveUseCase: EndDriveUseCase,
    private val deleteDriveUseCase: DeleteDriveUseCase,
    private val getPendingKmDriveUseCase: GetPendingKmDriveUseCase,
    private val resolvePendingKmUseCase: ResolvePendingKmUseCase,
    private val correctDriveUseCase: CorrectDriveUseCase,
    private val nextcloudSyncUseCase: NextcloudSyncUseCase,
    private val importDrivesUseCase: ImportDrivesUseCase,
    private val projectRepository: ProjectRepository,
    private val suggestionRepository: SuggestionRepository,
    private val locationHistory: LocationHistoryRepository,
    private val locationProvider: LocationProvider,
    private val geocoderProvider: GeocoderProvider,
    val prefs: AppPreferences
) : ViewModel() {

    val drives: StateFlow<List<Drive>> = getDrivesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Rit waarvan de eindstand nog ingevuld moet worden; niet-null = dialoog tonen.
    private val _pendingKm = MutableStateFlow<PendingKmUiState?>(null)
    val pendingKm: StateFlow<PendingKmUiState?> = _pendingKm.asStateFlow()

    val volvoStatus: StateFlow<VolvoStatus> = volvoConnectionUseCase.status

    private val _startDrive = MutableStateFlow<StartDriveUiState?>(null)
    val startDrive: StateFlow<StartDriveUiState?> = _startDrive.asStateFlow()

    // Project gekozen in de startdialoog; wordt aan de rit gekoppeld zodra die echt start.
    private var startProjectId: Long? = null

    val projects: StateFlow<List<Project>> = projectRepository.getProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Rit-id → project-ids waarvoor de gebruiker "nee" zei op het voorstel. */
    val rejectedSuggestions: StateFlow<Map<Long, Set<Long>>> = suggestionRepository.rejected
        .map { set -> set.groupBy({ it.driveId }, { it.projectId }).mapValues { it.value.toSet() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Thuis: ritpunten hier worden nooit aan een project gekoppeld. */
    private val _home = MutableStateFlow(locationHistory.getHomeLocation())
    val home: StateFlow<SavedLocation> = _home.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** Korte meldingen voor de snackbar. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch {
            // Bij het openen van de app direct de Volvo-verbinding tonen, en openstaande
            // eindstanden zo mogelijk stil invullen.
            volvoConnectionUseCase.check()
            loadPendingKm()
        }
    }

    fun checkVolvoConnection() {
        viewModelScope.launch { volvoConnectionUseCase.check() }
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

    // ── Rit starten ───────────────────────────────────────────────────────────

    fun startDrive(driveType: DriveType, projectId: Long? = null) {
        startProjectId = projectId
        _startDrive.value = StartDriveUiState.FetchingKm(driveType)
        fetchStartKm(driveType)
    }

    fun retryStartKm() {
        val current = _startDrive.value as? StartDriveUiState.VolvoFailed ?: return
        _startDrive.value = current.copy(isRetrying = true)
        fetchStartKm(current.driveType)
    }

    /** Startstand bevestigd door de gebruiker nadat Volvo faalde. */
    fun confirmStartKm(km: Int) {
        val current = _startDrive.value ?: return
        _startDrive.value = null
        viewModelScope.launch {
            startDriveUseCase(current.driveType, km, startProjectId)
            loadPendingKm()
        }
    }

    fun cancelStartDrive() {
        _startDrive.value = null
    }

    private fun fetchStartKm(driveType: DriveType) {
        viewModelScope.launch {
            val startKm = fetchStartKmUseCase()
            // Intussen geannuleerd: niets starten.
            if (_startDrive.value == null) return@launch
            when (val odometer = startKm.odometer) {
                is OdometerResult.Success -> {
                    _startDrive.value = null
                    startDriveUseCase(driveType, odometer.km, startProjectId)
                    loadPendingKm()
                }
                is OdometerResult.Failure -> _startDrive.value = StartDriveUiState.VolvoFailed(
                    driveType = driveType,
                    reason = odometer.reason,
                    suggestedKm = startKm.suggestedKm,
                )
            }
        }
    }

    fun endDrive() {
        // Een openstaande eindstand wordt bij de volgende app-start of rit-start vanzelf
        // ingevuld; direct na het beëindigen hoeft de gebruiker dus niets te doen.
        viewModelScope.launch {
            endDriveUseCase()
            if (nextcloudSyncUseCase.isConfigured) uploadToNextcloud()
        }
    }

    fun deleteDrive(driveId: Long) {
        viewModelScope.launch { deleteDriveUseCase(driveId) }
    }

    fun getDriveById(driveId: Long) = drives.value.firstOrNull { it.id == driveId }

    fun correctDrive(driveId: Long, correction: DriveCorrection) {
        viewModelScope.launch {
            correctDriveUseCase(driveId, correction)
            loadPendingKm()
        }
    }

    // ── Openstaande eindstand ─────────────────────────────────────────────────

    /**
     * Vult openstaande eindstanden stil in zodra er een nieuwe stand bekend is (startstand van de
     * volgende rit, of anders de huidige Volvo-stand). De dialoog verschijnt alleen als er al een
     * volgende rit is en de eindstand alsnog niet te bepalen is; zonder volgende rit wachten we
     * gewoon tot de auto gesynct heeft.
     */
    private fun loadPendingKm() {
        viewModelScope.launch {
            var pending = getPendingKmDriveUseCase()
            while (pending?.suggestedEndKm != null) {
                resolvePendingKmUseCase(pending.drive.id, pending.suggestedEndKm!!)
                pending = getPendingKmDriveUseCase()
            }
            _pendingKm.value = pending
                ?.takeIf { it.hasNextDrive }
                ?.let { PendingKmUiState(it.drive, it.suggestedEndKm) }
        }
    }

    fun syncPendingKm() {
        val current = _pendingKm.value ?: return
        _pendingKm.value = current.copy(isSyncing = true)
        viewModelScope.launch {
            // Handmatige sync: resultaat in de dialoog tonen, zodat de gebruiker het bevestigt.
            val pending = getPendingKmDriveUseCase()
            _pendingKm.value = pending?.let { PendingKmUiState(it.drive, it.suggestedEndKm) }
        }
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

    // ── Projecten ─────────────────────────────────────────────────────────────

    fun setDriveProjectAndNote(driveId: Long, projectId: Long?, note: String?) {
        viewModelScope.launch { correctDriveUseCase.setProjectAndNote(driveId, projectId, note) }
    }

    fun createProject(name: String, onCreated: (Project) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch { onCreated(projectRepository.findOrCreate(name)) }
    }

    /**
     * Maakt een project op de eind- (of start)locatie van [drive], koppelt die rit en geeft het
     * project terug zodat de UI meteen ritten op dezelfde plek kan voorstellen.
     */
    fun createProjectAtDriveLocation(drive: Drive, atEnd: Boolean, name: String, client: String?, onCreated: (Project) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val project = if (atEnd) {
                projectRepository.createAtLocation(name, client, drive.endLat, drive.endLng, drive.endAddress)
            } else {
                projectRepository.createAtLocation(name, client, drive.startLat, drive.startLng, drive.startAddress)
            }
            correctDriveUseCase.linkToProject(listOf(drive.id), project.id)
            onCreated(project)
        }
    }

    // ── Thuislocatie ──────────────────────────────────────────────────────────

    fun setHome(home: SavedLocation) {
        locationHistory.setHomeLocation(home)
        _home.value = home
        _messages.tryEmit("Thuislocatie: ${home.address}")
    }

    fun useCurrentLocationAsHome() {
        viewModelScope.launch {
            val gps = locationProvider.getCurrentLocation()
            if (gps == null) {
                _messages.tryEmit("Geen GPS-locatie beschikbaar")
                return@launch
            }
            val address = geocoderProvider.getAddress(gps.latitude, gps.longitude) ?: "Huidige locatie"
            setHome(SavedLocation(address, gps.latitude, gps.longitude))
        }
    }

    fun setHomeFromAddress(address: String) {
        if (address.isBlank()) return
        viewModelScope.launch {
            val point = geocoderProvider.getLocationFromAddress(address)
            if (point == null) _messages.tryEmit("Adres niet gevonden: $address")
            else setHome(SavedLocation(address.trim(), point.latitude, point.longitude))
        }
    }

    /** "Nee" op het voorstel: deze rit hoort niet bij dit project; niet meer voorstellen. */
    fun rejectSuggestion(driveId: Long, projectId: Long) {
        suggestionRepository.reject(listOf(Rejection(driveId, projectId)))
    }

    /** Koppelt [driveIds]; [rejectedIds] (uitgevinkt in het voorstel) worden niet meer voorgesteld. */
    fun linkDrivesToProject(driveIds: List<Long>, projectId: Long, rejectedIds: List<Long> = emptyList()) {
        suggestionRepository.reject(rejectedIds.map { Rejection(it, projectId) })
        if (driveIds.isEmpty()) return
        viewModelScope.launch {
            correctDriveUseCase.linkToProject(driveIds, projectId)
            _messages.tryEmit(if (driveIds.size == 1) "Rit gekoppeld" else "${driveIds.size} ritten gekoppeld")
        }
    }

    fun updateProjectDetails(projectId: Long, name: String, client: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { projectRepository.updateDetails(projectId, name, client) }
    }

    fun renameProject(projectId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { projectRepository.rename(projectId, name) }
    }

    fun setProjectArchived(projectId: Long, archived: Boolean) {
        viewModelScope.launch { projectRepository.setArchived(projectId, archived) }
    }

    // ── Nextcloud & import ────────────────────────────────────────────────────

    val nextcloudConfigured: Boolean get() = nextcloudSyncUseCase.isConfigured

    fun saveNextcloudSettings(url: String, username: String, password: String) {
        prefs.nextcloudUrl = url.trim()
        prefs.nextcloudUsername = username.trim()
        prefs.nextcloudPassword = password
    }

    fun syncToNextcloud() {
        viewModelScope.launch { uploadToNextcloud() }
    }

    private suspend fun uploadToNextcloud() {
        _syncStatus.value = SyncStatus.Syncing
        nextcloudSyncUseCase.upload()
            .onSuccess { _syncStatus.value = SyncStatus.Done(System.currentTimeMillis()) }
            .onFailure { _syncStatus.value = SyncStatus.Failed(it.message ?: "Onbekende fout") }
    }

    fun restoreFromNextcloud() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            nextcloudSyncUseCase.restore()
                .onSuccess {
                    _syncStatus.value = SyncStatus.Done(System.currentTimeMillis())
                    _messages.tryEmit(importMessage(it))
                }
                .onFailure {
                    _syncStatus.value = SyncStatus.Failed(it.message ?: "Onbekende fout")
                    _messages.tryEmit("Herstellen mislukt: ${it.message}")
                }
        }
    }

    fun importFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val csv = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("Bestand niet leesbaar")
                importDrivesUseCase(csv)
            }
                .onSuccess { _messages.tryEmit(importMessage(it)) }
                .onFailure { _messages.tryEmit("Importeren mislukt: ${it.message}") }
        }
    }

    private fun importMessage(r: ImportDrivesUseCase.Result) =
        "${r.imported} ritten toegevoegd, ${r.skipped} bestonden al"

    fun saveSettings(vin: String, apiKey: String, clientId: String, clientSecret: String) {
        prefs.vin = vin
        prefs.vccApiKey = apiKey
        prefs.clientId = clientId
        prefs.clientSecret = clientSecret
    }
}
