package com.rykle.kmregistratie.ui.screen

import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveCorrection
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.ui.theme.MismatchRed
import com.rykle.kmregistratie.ui.viewmodel.DriveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveDetailScreen(
    driveId: Long,
    viewModel: DriveViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val drives by viewModel.drives.collectAsState()
    val drive = drives.firstOrNull { it.id == driveId }
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showKmDialog by remember { mutableStateOf(false) }
    val previousDrive = drive?.let { d ->
        drives.filter { it.startTime < d.startTime }.maxByOrNull { it.startTime }
    }
    val nextDrive = drive?.let { d ->
        drives.filter { it.startTime > d.startTime }.minByOrNull { it.startTime }
    }

    val startAddress by produceState("…", drive?.startAddress, drive?.startLat, drive?.startLng) {
        value = drive?.startAddress
            ?: reverseGeocode(context, drive?.startLat ?: 0.0, drive?.startLng ?: 0.0)
            ?: formatCoords(drive?.startLat, drive?.startLng)
            ?: "Onbekend"
    }
    val endAddress by produceState("…", drive?.endAddress, drive?.endLat, drive?.endLng) {
        value = drive?.endAddress
            ?: reverseGeocode(context, drive?.endLat ?: 0.0, drive?.endLng ?: 0.0)
            ?: formatCoords(drive?.endLat, drive?.endLng)
            ?: "Onbekend"
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Rit verwijderen") },
            text = { Text("Weet je zeker dat je deze rit wilt verwijderen? Dit kan niet ongedaan worden gemaakt.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDrive(driveId)
                        showDeleteDialog = false
                        onDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Verwijderen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuleren") }
            }
        )
    }

    if (showKmDialog && drive != null) {
        CorrectDriveDialog(
            drive = drive,
            startAddressShown = startAddress,
            endAddressShown = endAddress,
            previousDrive = previousDrive,
            nextDrive = nextDrive,
            onDismiss = { showKmDialog = false },
            onSave = { correction ->
                viewModel.correctDrive(driveId, correction)
                showKmDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (drive?.driveType == DriveType.BUSINESS) "Zakelijke rit" else "Privérit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (drive == null) {
            Text(
                text = "Rit niet gevonden",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (drive.hasKmMismatch) {
                Text(
                    text = "⚠ KM-mismatch: start ${drive.startKm ?: "?"} km, " +
                        "vorige rit eindigde op ${previousDrive?.endKm ?: "?"} km",
                    style = MaterialTheme.typography.labelLarge,
                    color = MismatchRed,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            InfoSection(title = "Algemeen") {
                InfoRow("Type", if (drive.driveType == DriveType.BUSINESS) "Zakelijk" else "Privé")
                InfoRow("Status", if (drive.isActive) "Actief" else "Afgerond")
                drive.kmDriven?.let { InfoRow("Gereden", "$it km") }
            }

            Spacer(Modifier.height(16.dp))
            InfoSection(title = "Tijd") {
                InfoRow("Start", formatDateTime(drive.startTime))
                if (drive.endTime != null) {
                    InfoRow("Einde", formatDateTime(drive.endTime))
                    InfoRow("Duur", formatDuration(drive.endTime - drive.startTime))
                }
            }

            Spacer(Modifier.height(16.dp))
            InfoSection(title = "Kilometerstand") {
                InfoRow("Start km", drive.startKm?.toString() ?: "onbekend")
                InfoRow(
                    "Einde km",
                    when {
                        drive.isActive -> "nog actief"
                        drive.kmPending -> "nog invullen"
                        else -> drive.endKm?.toString() ?: "onbekend"
                    }
                )
                OutlinedButton(
                    onClick = { showKmDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Rit corrigeren (km / locatie)") }
            }

            Spacer(Modifier.height(16.dp))
            InfoSection(title = "Locatie start") {
                InfoRow("Adres", startAddress)
            }

            if (!drive.isActive) {
                Spacer(Modifier.height(16.dp))
                InfoSection(title = "Locatie einde") {
                    InfoRow("Adres", endAddress)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Rit verwijderen")
            }
        }
    }
}

@Composable
private fun CorrectDriveDialog(
    drive: Drive,
    startAddressShown: String,
    endAddressShown: String,
    previousDrive: Drive?,
    nextDrive: Drive?,
    onDismiss: () -> Unit,
    onSave: (DriveCorrection) -> Unit,
) {
    fun usable(shown: String) = shown.takeUnless { it == "…" || it == "Onbekend" } ?: ""

    var startKmText by remember { mutableStateOf(drive.startKm?.toString() ?: "") }
    var endKmText by remember { mutableStateOf(drive.endKm?.toString() ?: "") }
    var startAddressText by remember { mutableStateOf(drive.startAddress ?: usable(startAddressShown)) }
    var endAddressText by remember { mutableStateOf(drive.endAddress ?: usable(endAddressShown)) }
    var startLat by remember { mutableStateOf(drive.startLat) }
    var startLng by remember { mutableStateOf(drive.startLng) }
    var endLat by remember { mutableStateOf(drive.endLat) }
    var endLng by remember { mutableStateOf(drive.endLng) }

    val startKm = startKmText.toIntOrNull()
    val endKm = endKmText.toIntOrNull()
    val orderValid = startKm == null || endKm == null || endKm >= startKm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rit corrigeren") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text("Start", style = MaterialTheme.typography.titleSmall)
                if (previousDrive != null && !previousDrive.isActive) {
                    SuggestionButton(
                        label = "Overnemen van einde vorige rit",
                        km = previousDrive.endKm,
                        address = previousDrive.endAddress,
                    ) {
                        previousDrive.endKm?.let { startKmText = it.toString() }
                        previousDrive.endAddress?.let { startAddressText = it }
                        if (previousDrive.endLat != null && previousDrive.endLng != null) {
                            startLat = previousDrive.endLat
                            startLng = previousDrive.endLng
                        }
                    }
                }
                KmField("Start km", startKmText) { startKmText = it }
                OutlinedTextField(
                    value = startAddressText,
                    onValueChange = { startAddressText = it },
                    label = { Text("Startadres") },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!drive.isActive) {
                    Spacer(Modifier.height(8.dp))
                    Text("Einde", style = MaterialTheme.typography.titleSmall)
                    if (nextDrive != null) {
                        SuggestionButton(
                            label = "Overnemen van start volgende rit",
                            km = nextDrive.startKm,
                            address = nextDrive.startAddress,
                        ) {
                            nextDrive.startKm?.let { endKmText = it.toString() }
                            nextDrive.startAddress?.let { endAddressText = it }
                            if (nextDrive.startLat != 0.0 || nextDrive.startLng != 0.0) {
                                endLat = nextDrive.startLat
                                endLng = nextDrive.startLng
                            }
                        }
                    }
                    KmField("Einde km", endKmText, isError = !orderValid) { endKmText = it }
                    if (!orderValid) {
                        Text(
                            "Eindstand moet hoger zijn dan startstand",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedTextField(
                        value = endAddressText,
                        onValueChange = { endAddressText = it },
                        label = { Text("Eindadres") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        DriveCorrection(
                            startKm = startKm,
                            endKm = endKm,
                            startLat = startLat,
                            startLng = startLng,
                            startAddress = startAddressText.trim().ifBlank { null },
                            endLat = endLat,
                            endLng = endLng,
                            endAddress = endAddressText.trim().ifBlank { null },
                        )
                    )
                },
                enabled = orderValid,
            ) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        },
    )
}

@Composable
private fun SuggestionButton(label: String, km: Int?, address: String?, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                listOfNotNull(km?.let { "$it km" }, address).joinToString(" · ").ifBlank { "geen gegevens" },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun KmField(label: String, value: String, isError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Suppress("DEPRECATION")
private fun formatCoords(lat: Double?, lng: Double?): String? {
    if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) return null
    return "%.5f, %.5f".format(lat, lng)
}

private suspend fun reverseGeocode(context: android.content.Context, lat: Double, lng: Double): String? {
    if (lat == 0.0 && lng == 0.0) return null
    return withContext(Dispatchers.IO) {
        try {
            val results = Geocoder(context, Locale("nl", "NL")).getFromLocation(lat, lng, 1)
            val address = results?.firstOrNull() ?: return@withContext null
            buildString {
                val street = address.thoroughfare
                if (street != null) {
                    append(street)
                    address.subThoroughfare?.let { append(" $it") }
                }
                val city = address.locality ?: address.subAdminArea
                if (city != null) {
                    if (isNotEmpty()) append(", ")
                    append(city)
                }
            }.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    content()
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDateTime(ms: Long): String =
    SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL")).format(Date(ms))

private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return if (hours > 0) "${hours}u ${minutes}m" else "${minutes}m"
}
