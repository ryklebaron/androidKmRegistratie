package com.rykle.kmregistratie.service

import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
import android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.model.LocationPoint
import com.rykle.kmregistratie.domain.repository.DriveRepository
import com.rykle.kmregistratie.domain.usecase.ContinueDriveUseCase
import com.rykle.kmregistratie.domain.usecase.DriveStartData
import com.rykle.kmregistratie.domain.usecase.EndDriveUseCase
import com.rykle.kmregistratie.domain.usecase.FetchDriveStartDataUseCase
import com.rykle.kmregistratie.domain.usecase.StartDriveUseCase
import com.rykle.kmregistratie.ui.theme.KmRegistratieTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private sealed interface PickerUiState {
    data object Loading : PickerUiState
    data class ActiveDriveWarning(
        val activeDrive: Drive,
        val suggestedEndKm: Int?,
    ) : PickerUiState
    data object Picking : PickerUiState
    data class KmConfirmation(
        val driveType: DriveType,
        val location: LocationPoint?,
        val lastKm: Int?,
    ) : PickerUiState
}

@AndroidEntryPoint
class DrivePickerActivity : ComponentActivity() {

    @Inject lateinit var startDriveUseCase: StartDriveUseCase
    @Inject lateinit var continueDriveUseCase: ContinueDriveUseCase
    @Inject lateinit var endDriveUseCase: EndDriveUseCase
    @Inject lateinit var connectionTracker: ConnectionTracker
    @Inject lateinit var fetchDriveStartDataUseCase: FetchDriveStartDataUseCase
    @Inject lateinit var driveRepository: DriveRepository

    private var uiState by mutableStateOf<PickerUiState>(PickerUiState.Loading)
    private var cachedStartData: DriveStartData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(FLAG_SHOW_WHEN_LOCKED or FLAG_TURN_SCREEN_ON)
        }
        super.onCreate(savedInstanceState)

        loadInitialState()

        setContent {
            KmRegistratieTheme {
                DrivePickerOverlay(
                    uiState = uiState,
                    canContinue = connectionTracker.canContinueDrive(),
                    onBusiness = { pick(DriveType.BUSINESS) },
                    onPrivate = { pick(DriveType.PRIVATE) },
                    onContinue = { continueDrive() },
                    onDismiss = ::finish,
                    onKmConfirmed = { driveType, location, km -> startWithKm(driveType, location, km) },
                    onKmCancelled = { uiState = PickerUiState.Picking },
                    onActiveDriveClosed = { endKm -> closeActiveDriveAndContinue(endKm) },
                )
            }
        }
    }

    private fun loadInitialState() {
        lifecycleScope.launch {
            val activeDrive = withContext(Dispatchers.IO) { driveRepository.getActiveDrive() }
            val startData = withContext(Dispatchers.IO) { fetchDriveStartDataUseCase() }
            cachedStartData = startData
            uiState = if (activeDrive != null) {
                PickerUiState.ActiveDriveWarning(
                    activeDrive = activeDrive,
                    suggestedEndKm = startData.km ?: startData.lastKm,
                )
            } else {
                PickerUiState.Picking
            }
        }
    }

    private fun closeActiveDriveAndContinue(endKm: Int?) {
        uiState = PickerUiState.Loading
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { endDriveUseCase(endKm) }
            // If API was unavailable, use the manually entered km as the lastKm hint for new drive
            if (endKm != null) {
                cachedStartData = cachedStartData?.let { data ->
                    data.copy(lastKm = data.km ?: endKm)
                }
            }
            uiState = PickerUiState.Picking
        }
    }

    private fun pick(type: DriveType) {
        val data = cachedStartData ?: return
        if (data.km != null) {
            uiState = PickerUiState.Loading
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { startDriveUseCase(type, data.location, null, data.km) }
                cancelPickerNotification(this@DrivePickerActivity)
                finish()
            }
        } else {
            uiState = PickerUiState.KmConfirmation(type, data.location, data.lastKm)
        }
    }

    private fun startWithKm(type: DriveType, location: LocationPoint?, km: Int?) {
        uiState = PickerUiState.Loading
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { startDriveUseCase(type, location, null, km) }
            cancelPickerNotification(this@DrivePickerActivity)
            finish()
        }
    }

    private fun continueDrive() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                continueDriveUseCase()
                connectionTracker.clearProvisionalEnd()
            }
            cancelPickerNotification(this@DrivePickerActivity)
            finish()
        }
    }
}

@Composable
private fun DrivePickerOverlay(
    uiState: PickerUiState,
    canContinue: Boolean,
    onBusiness: () -> Unit,
    onPrivate: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    onKmConfirmed: (DriveType, LocationPoint?, Int?) -> Unit,
    onKmCancelled: () -> Unit,
    onActiveDriveClosed: (Int?) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            when (uiState) {
                PickerUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PickerUiState.ActiveDriveWarning -> {
                    ActiveDriveWarningContent(
                        state = uiState,
                        onConfirm = onActiveDriveClosed,
                        onDismiss = onDismiss,
                    )
                }

                PickerUiState.Picking -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.screen_title_select_drive),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onBusiness,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            Text(
                                stringResource(R.string.drive_type_business),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Button(
                            onClick = onPrivate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            Text(
                                stringResource(R.string.drive_type_private),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        if (canContinue) {
                            OutlinedButton(
                                onClick = onContinue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    stringResource(R.string.drive_type_continue),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.picker_dismiss))
                        }
                    }
                }

                is PickerUiState.KmConfirmation -> {
                    KmConfirmationContent(
                        state = uiState,
                        onConfirm = onKmConfirmed,
                        onCancel = onKmCancelled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveDriveWarningContent(
    state: PickerUiState.ActiveDriveWarning,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var kmText by rememberSaveable { mutableStateOf(state.suggestedEndKm?.toString() ?: "") }

    val startTimeFormatted = remember(state.activeDrive.startTime) {
        DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(state.activeDrive.startTime))
    }
    val driveTypeLabel = stringResource(
        when (state.activeDrive.driveType) {
            DriveType.BUSINESS -> R.string.active_drive_type_business
            DriveType.PRIVATE -> R.string.active_drive_type_private
        }
    )

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.active_drive_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(
                R.string.active_drive_desc,
                driveTypeLabel,
                stringResource(R.string.active_drive_started_at, startTimeFormatted),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.suggestedEndKm != null) {
            Text(
                text = stringResource(R.string.active_drive_km_suggestion, state.suggestedEndKm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedTextField(
            value = kmText,
            onValueChange = { kmText = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.active_drive_end_km_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = { onConfirm(kmText.toIntOrNull()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(
                stringResource(R.string.active_drive_close_btn),
                style = MaterialTheme.typography.titleMedium
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Text(stringResource(R.string.picker_dismiss))
        }
    }
}

@Composable
private fun KmConfirmationContent(
    state: PickerUiState.KmConfirmation,
    onConfirm: (DriveType, LocationPoint?, Int?) -> Unit,
    onCancel: () -> Unit,
) {
    var kmText by rememberSaveable { mutableStateOf(state.lastKm?.toString() ?: "") }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.km_fallback_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.km_fallback_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        OutlinedTextField(
            value = kmText,
            onValueChange = { kmText = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.km_fallback_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(
            onClick = { onConfirm(state.driveType, state.location, kmText.toIntOrNull()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.km_fallback_confirm))
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        ) {
            Text(stringResource(R.string.km_fallback_cancel))
        }
    }
}
