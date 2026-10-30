package com.rykle.kmregistratie.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.rykle.kmregistratie.ui.viewmodel.StartDriveUiState

@Composable
fun StartKmDialog(
    state: StartDriveUiState,
    onConfirm: (Int) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val type = if (state.driveType == DriveType.BUSINESS) "Zakelijke" else "Privé"

    when (state) {
        is StartDriveUiState.FetchingKm -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("$type rit starten") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Km-stand ophalen bij Volvo…")
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onCancel) { Text("Annuleren") } },
        )

        is StartDriveUiState.VolvoFailed -> {
            // Voorstel staat vast (grijs) tot de gebruiker op het potlood tikt.
            var editing by remember { mutableStateOf(state.suggestedKm == null) }
            var kmText by remember(state.suggestedKm) {
                mutableStateOf(state.suggestedKm?.toString() ?: "")
            }
            val enteredKm = kmText.toIntOrNull()

            AlertDialog(
                onDismissRequest = onCancel,
                properties = DialogProperties(dismissOnClickOutside = false),
                title = { Text("$type rit starten") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Column {
                                Text(
                                    "Km-stand ophalen bij Volvo is mislukt",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(state.reason, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text(
                            if (state.suggestedKm != null) {
                                "Voorstel: de eindstand van de vorige rit. Tik op het potlood " +
                                    "om aan te passen."
                            } else {
                                "Er is geen vorige stand bekend. Vul de km-stand zelf in."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedTextField(
                            value = kmText,
                            onValueChange = { kmText = it.filter { c -> c.isDigit() } },
                            label = { Text("Startstand (km)") },
                            enabled = editing,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = if (!editing) {
                                {
                                    IconButton(onClick = { editing = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Aanpassen")
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { enteredKm?.let(onConfirm) },
                            enabled = enteredKm != null && !state.isRetrying,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Start rit met deze stand") }
                        OutlinedButton(
                            onClick = onRetry,
                            enabled = !state.isRetrying,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.isRetrying) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Opnieuw proberen bij Volvo")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = onCancel) { Text("Annuleren") } },
            )
        }
    }
}
