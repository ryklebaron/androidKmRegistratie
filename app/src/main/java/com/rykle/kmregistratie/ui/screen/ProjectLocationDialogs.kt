package com.rykle.kmregistratie.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rykle.kmregistratie.domain.model.Drive
import com.rykle.kmregistratie.domain.model.DriveType
import com.rykle.kmregistratie.domain.model.Project
import com.rykle.kmregistratie.domain.model.ProjectLocations
import com.rykle.kmregistratie.domain.model.SavedLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Eind- of startlocatie van een rit vastleggen als (klant)project. */
@Composable
fun SaveLocationAsProjectDialog(
    drive: Drive,
    home: SavedLocation?,
    onDismiss: () -> Unit,
    onCreate: (atEnd: Boolean, name: String, client: String?) -> Unit,
) {
    val canUseEnd = !drive.isActive
    // Standaard het punt dat níet thuis is: meestal de bestemming, bij een terugrit het vertrekpunt.
    var atEnd by remember {
        mutableStateOf(canUseEnd && !ProjectLocations.isHome(home, drive.endLat, drive.endLng, drive.endAddress))
    }
    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    val address = (if (atEnd) drive.endAddress else drive.startAddress) ?: "adres onbekend (alleen GPS)"
    val isHome = if (atEnd) ProjectLocations.isHome(home, drive.endLat, drive.endLng, drive.endAddress)
                 else ProjectLocations.isHome(home, drive.startLat, drive.startLng, drive.startAddress)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Locatie opslaan als project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canUseEnd) {
                        FilterChip(selected = atEnd, onClick = { atEnd = true }, label = { Text("Bestemming") })
                    }
                    FilterChip(selected = !atEnd, onClick = { atEnd = false }, label = { Text("Vertrekpunt") })
                }
                Text(address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                if (isHome) {
                    Text(
                        "Dit is je thuislocatie. Kies het andere punt van de rit (de klant).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Projectnaam") },
                    placeholder = { Text("bv. Zonnepanelen Jansen") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Klant (optioneel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Deze rit wordt gekoppeld; daarna zie je welke andere ritten op deze plek begonnen of eindigden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(atEnd, name, client) }, enabled = name.isNotBlank() && !isHome) { Text("Opslaan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

/**
 * Voorstel: ritten die bij de projectlocatie aankwamen of vertrokken. Ritten zonder project staan
 * aangevinkt; ritten bij een ander project niet. Koppelen zet alleen het projectlabel.
 */
@Composable
fun NearbyDrivesDialog(
    project: Project,
    drives: List<Drive>,
    projectNames: Map<Long, String>,
    home: SavedLocation?,
    onDismiss: () -> Unit,
    onLink: (linked: List<Long>, rejected: List<Long>) -> Unit,
) {
    val matches = remember(project, drives, home) {
        ProjectLocations.nearbyDrives(project, drives, home).filter { it.drive.projectId != project.id }
    }
    val alreadyLinked = remember(project, drives) { drives.count { it.projectId == project.id } }
    var checked by remember(matches) {
        mutableStateOf(matches.filter { it.drive.projectId == null }.map { it.drive.id }.toSet())
    }
    val fmt = remember { SimpleDateFormat("EEE dd-MM-yyyy HH:mm", Locale("nl", "NL")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ritten bij ${project.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (matches.isEmpty()) "Geen andere ritten gevonden die hier begonnen of eindigden."
                    else "${matches.size} ritten begonnen of eindigden hier (binnen ${ProjectLocations.RADIUS_METERS.toInt()} m)." +
                        if (alreadyLinked > 0) " $alreadyLinked al gekoppeld." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (matches.size > 1) {
                    TextButton(
                        onClick = {
                            checked = if (checked.size == matches.size) emptySet() else matches.map { it.drive.id }.toSet()
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text(if (checked.size == matches.size) "Niets selecteren" else "Alles selecteren") }
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(matches, key = { it.drive.id }) { m ->
                        val d = m.drive
                        val on = d.id in checked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checked = if (on) checked - d.id else checked + d.id }
                                .padding(vertical = 2.dp),
                        ) {
                            Checkbox(checked = on, onCheckedChange = { checked = if (on) checked - d.id else checked + d.id })
                            Column(Modifier.weight(1f)) {
                                Text(fmt.format(Date(d.startTime)), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    listOfNotNull(
                                        when {
                                            m.arrives && m.departs -> "heen en terug"
                                            m.arrives -> "heen"
                                            else -> "terug"
                                        },
                                        if (d.driveType == DriveType.BUSINESS) "zakelijk" else "privé",
                                        d.projectId?.let { "nu bij ${projectNames[it] ?: "ander project"}" },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (d.projectId != null) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(d.kmDriven?.let { "$it km" } ?: "? km", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Uitgevinkte ritten zonder project gelden als "hoort er niet bij".
            Button(
                onClick = {
                    onLink(checked.toList(), matches.filter { it.drive.projectId == null && it.drive.id !in checked }.map { it.drive.id })
                },
                enabled = checked.isNotEmpty(),
            ) {
                Text(if (checked.size == 1) "Koppel 1 rit" else "Koppel ${checked.size} ritten")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (matches.isEmpty()) "Sluiten" else "Overslaan") } },
    )
}
