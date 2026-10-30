package com.rykle.kmregistratie.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.KeyboardType
import com.rykle.kmregistratie.ui.viewmodel.SyncStatus
import com.rykle.kmregistratie.domain.model.ProjectLocations
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.ui.VolvoOAuthActivity
import com.rykle.kmregistratie.ui.viewmodel.DriveViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: DriveViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var vin by remember { mutableStateOf(viewModel.prefs.vin) }
    var apiKey by remember { mutableStateOf(viewModel.prefs.vccApiKey) }
    var clientId by remember { mutableStateOf(viewModel.prefs.clientId) }
    var clientSecret by remember { mutableStateOf(viewModel.prefs.clientSecret) }
    var isLinked by remember { mutableStateOf(viewModel.prefs.refreshToken.isNotBlank()) }
    var editing by remember {
        mutableStateOf(listOf(vin, apiKey, clientId, clientSecret).any { it.isBlank() })
    }

    fun restoreSaved() {
        vin = viewModel.prefs.vin
        apiKey = viewModel.prefs.vccApiKey
        clientId = viewModel.prefs.clientId
        clientSecret = viewModel.prefs.clientSecret
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLinked = viewModel.prefs.refreshToken.isNotBlank()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_title_settings),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = vin,
            onValueChange = { vin = it },
            label = { Text(stringResource(R.string.settings_vin_label)) },
            enabled = editing,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.settings_api_key_label)) },
            enabled = editing,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text(stringResource(R.string.settings_client_id_label)) },
            enabled = editing,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clientSecret,
            onValueChange = { clientSecret = it },
            label = { Text(stringResource(R.string.settings_client_secret_label)) },
            enabled = editing,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        restoreSaved()
                        editing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
                Button(
                    onClick = {
                        viewModel.saveSettings(vin.trim(), apiKey.trim(), clientId.trim(), clientSecret.trim())
                        restoreSaved()
                        editing = false
                        scope.launch { snackbarHostState.showSnackbar("✓ Instellingen opgeslagen") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }
        } else {
            Text(
                text = stringResource(R.string.settings_saved_locked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { editing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_edit))
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.volvo_link_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isLinked) stringResource(R.string.volvo_link_status_linked)
                   else stringResource(R.string.volvo_link_status_not_linked),
            style = MaterialTheme.typography.bodySmall,
            color = if (isLinked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        val canLink = !editing && clientId.isNotBlank() && clientSecret.isNotBlank()
        Button(
            onClick = { context.startActivity(Intent(context, VolvoOAuthActivity::class.java)) },
            enabled = canLink,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (isLinked) stringResource(R.string.volvo_link_relink)
                else stringResource(R.string.volvo_link_btn)
            )
        }
        if (!canLink) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.volvo_link_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        HomeSection(viewModel)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        NextcloudSection(viewModel, snackbarHostState)

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        ImportSection(viewModel)
    }
}

@Composable
private fun NextcloudSection(viewModel: DriveViewModel, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var url by remember { mutableStateOf(viewModel.prefs.nextcloudUrl) }
    var username by remember { mutableStateOf(viewModel.prefs.nextcloudUsername) }
    var password by remember { mutableStateOf(viewModel.prefs.nextcloudPassword) }
    var configured by remember { mutableStateOf(viewModel.nextcloudConfigured) }
    var editing by remember { mutableStateOf(!configured) }
    var confirmRestore by remember { mutableStateOf(false) }
    val busy = syncStatus is SyncStatus.Syncing

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Ritten ophalen van Nextcloud") },
            text = {
                Text(
                    "Ritten uit km_ritten.csv die hier nog niet staan worden toegevoegd. " +
                        "Bestaande ritten blijven precies zoals ze zijn; er wordt niets gewist."
                )
            },
            confirmButton = {
                Button(onClick = { confirmRestore = false; viewModel.restoreFromNextcloud() }) { Text("Ophalen") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Annuleren") } },
        )
    }

    Text("Nextcloud synchronisatie", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Na elke rit wordt de volledige rittenlijst (met projecten en notities) als km_ritten.csv " +
            "in je Nextcloud gezet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = url,
        onValueChange = { url = it },
        label = { Text("Nextcloud-URL") },
        placeholder = { Text("https://cloud.voorbeeld.nl") },
        supportingText = {
            Text("Het adres waarmee je Nextcloud in de browser opent. Het WebDAV-adres (…/remote.php/dav/files/…) mag ook.")
        },
        enabled = editing,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Gebruikersnaam") },
        enabled = editing,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("App-wachtwoord") },
        enabled = editing,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))

    if (editing) {
        Button(
            onClick = {
                viewModel.saveNextcloudSettings(url, username, password)
                configured = viewModel.nextcloudConfigured
                editing = !configured
                scope.launch { snackbarHostState.showSnackbar("✓ Nextcloud-instellingen opgeslagen") }
            },
            enabled = url.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Nextcloud opslaan") }
    } else {
        Text(
            text = when (val st = syncStatus) {
                SyncStatus.Idle -> "Ingesteld"
                SyncStatus.Syncing -> "Bezig met synchroniseren…"
                is SyncStatus.Done -> "✓ Gesynchroniseerd om ${SimpleDateFormat("HH:mm", Locale("nl", "NL")).format(Date(st.atMillis))}"
                is SyncStatus.Failed -> "⚠ ${st.reason}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (syncStatus is SyncStatus.Failed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::syncToNextcloud,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Nu synchroniseren") }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { confirmRestore = true },
                enabled = !busy,
                modifier = Modifier.weight(1f),
            ) { Text("Ritten ophalen") }
            OutlinedButton(
                onClick = { editing = true },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.settings_edit)) }
        }
    }
}

@Composable
private fun HomeSection(viewModel: DriveViewModel) {
    val home by viewModel.home.collectAsState()
    val drives by viewModel.drives.collectAsState()
    val guess = remember(drives) { ProjectLocations.guessHome(drives) }
    var typed by remember { mutableStateOf("") }
    // Alleen voorstellen als het echt een andere plek is dan wat nu is ingesteld.
    val guessDiffers = guess != null && ProjectLocations.distanceMeters(
        home.latitude, home.longitude, guess.first.latitude, guess.first.longitude,
    ) > 30

    Text("Thuislocatie", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Ritten die thuis beginnen of eindigen worden daar nooit aan een project gekoppeld. " +
            "Alles binnen ${ProjectLocations.HOME_RADIUS_METERS.toInt()} m telt als thuis.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Text(home.address, style = MaterialTheme.typography.bodyLarge)
    Text(
        "%.5f, %.5f".format(Locale.ROOT, home.latitude, home.longitude),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (guessDiffers && guess != null) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.setHome(guess.first) }, modifier = Modifier.fillMaxWidth()) {
            Text("Uit je ritten: ${guess.first.address} (${guess.second}×)")
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = viewModel::useCurrentLocationAsHome, modifier = Modifier.fillMaxWidth()) {
        Text("Huidige locatie gebruiken")
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { Text("Of typ een adres") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { viewModel.setHomeFromAddress(typed); typed = "" }, enabled = typed.isNotBlank()) {
            Text("Instellen")
        }
    }
}

@Composable
private fun ImportSection(viewModel: DriveViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFromFile(context, uri)
    }
    Text("Importeren", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "Ritten uit een eerder geëxporteerde CSV toevoegen. Ritten die al bestaan worden overgeslagen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("CSV-bestand kiezen") }
}
