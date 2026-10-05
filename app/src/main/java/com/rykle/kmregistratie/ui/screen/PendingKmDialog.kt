package com.rykle.kmregistratie.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.ui.viewmodel.PendingKmUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PendingKmDialog(
    state: PendingKmUiState,
    onConfirm: (Int) -> Unit,
    onSync: () -> Unit,
    onPostpone: () -> Unit,
    onDelete: () -> Unit,
) {
    val drive = state.drive
    val startKm = drive.startKm

    // Opnieuw vullen zodra een sync een (nieuwe) suggestie oplevert; daarna vrij aanpasbaar.
    var kmText by remember(drive.id, state.suggestedEndKm) {
        mutableStateOf(state.suggestedEndKm?.toString() ?: "")
    }
    var confirmDelete by remember(drive.id) { mutableStateOf(false) }

    val enteredKm = kmText.toIntOrNull()
    val isValid = enteredKm != null && (startKm == null || enteredKm >= startKm)

    val fmt = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("nl", "NL")) }
    val type = if (drive.driveType == DriveType.BUSINESS) "Zakelijke" else "Privé"

    AlertDialog(
        onDismissRequest = onPostpone,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Eindstand invullen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "$type rit van ${fmt.format(Date(drive.startTime))}" +
                        (startKm?.let { " — startstand $it km." } ?: "."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.suggestedEndKm != null) {
                    Text(
                        "Nieuwe km-stand gevonden. Controleer en bevestig.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        "Volvo heeft nog geen nieuwe km-stand. De auto moet even vergrendeld " +
                            "staan voordat de stand naar de cloud gaat. Vul de stand zelf in, " +
                            "synchroniseer opnieuw of doe het later.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                    label = { Text("Eindstand (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = kmText.isNotEmpty() && !isValid,
                    supportingText = if (kmText.isNotEmpty() && !isValid && startKm != null) {
                        { Text("Mag niet lager zijn dan de startstand ($startKm)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { enteredKm?.let(onConfirm) },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Opslaan") }
                OutlinedButton(
                    onClick = onSync,
                    enabled = !state.isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.isSyncing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Opnieuw synchroniseren")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(if (confirmDelete) "Zeker? Verwijder rit" else "Rit verwijderen") }
                    TextButton(onClick = onPostpone) { Text("Later invullen") }
                }
            }
        },
        confirmButton = {},
    )
}
