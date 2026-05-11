package com.rykle.kmregistratie.ui.screen

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.ui.viewmodel.DriveViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: DriveViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var vin by remember { mutableStateOf(viewModel.prefs.vin) }
    var apiKey by remember { mutableStateOf(viewModel.prefs.vccApiKey) }
    var clientId by remember { mutableStateOf(viewModel.prefs.clientId) }
    var clientSecret by remember { mutableStateOf(viewModel.prefs.clientSecret) }
    var isLinked by remember { mutableStateOf(viewModel.prefs.refreshToken.isNotBlank()) }
    var nextcloudUrl by remember { mutableStateOf(viewModel.prefs.nextcloudUrl) }
    var nextcloudUsername by remember { mutableStateOf(viewModel.prefs.nextcloudUsername) }
    var nextcloudPassword by remember { mutableStateOf(viewModel.prefs.nextcloudPassword) }

    val notifManager = context.getSystemService(NotificationManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotifPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var canUseFullScreenIntent by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notifManager.canUseFullScreenIntent()
        )
    }
    var isBatteryOptimized by remember {
        mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotifPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    canUseFullScreenIntent = notifManager.canUseFullScreenIntent()
                }
                isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
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

        if (!hasNotifPermission) {
            PermissionBanner(
                title = stringResource(R.string.perm_notif_title),
                desc = stringResource(R.string.perm_notif_desc),
                buttonLabel = stringResource(R.string.perm_notif_button),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        if (!canUseFullScreenIntent) {
            PermissionBanner(
                title = stringResource(R.string.perm_fullscreen_title),
                desc = stringResource(R.string.perm_fullscreen_desc),
                buttonLabel = stringResource(R.string.perm_fullscreen_button),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isBatteryOptimized) {
            PermissionBanner(
                title = stringResource(R.string.perm_battery_title),
                desc = stringResource(R.string.perm_battery_desc),
                buttonLabel = stringResource(R.string.perm_battery_button),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { viewModel.postTestPickerNotification(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.test_notification_button))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = vin,
            onValueChange = { vin = it },
            label = { Text(stringResource(R.string.settings_vin_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.settings_api_key_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text(stringResource(R.string.settings_client_id_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clientSecret,
            onValueChange = { clientSecret = it },
            label = { Text(stringResource(R.string.settings_client_secret_label)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.saveSettings(vin, apiKey, clientId, clientSecret)
                scope.launch { snackbarHostState.showSnackbar("Instellingen opgeslagen") }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_save))
        }

        Spacer(Modifier.height(16.dp))

        VolvoLinkSection(
            isLinked = isLinked,
            canLink = clientId.isNotBlank() && clientSecret.isNotBlank(),
            onLink = {
                viewModel.saveSettings(vin, apiKey, clientId, clientSecret)
                context.startActivity(
                    android.content.Intent(context, com.rykle.kmregistratie.service.VolvoOAuthActivity::class.java)
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        NextcloudSection(
            url = nextcloudUrl,
            username = nextcloudUsername,
            password = nextcloudPassword,
            onUrlChange = { nextcloudUrl = it },
            onUsernameChange = { nextcloudUsername = it },
            onPasswordChange = { nextcloudPassword = it },
            onSave = {
                viewModel.saveNextcloudSettings(nextcloudUrl, nextcloudUsername, nextcloudPassword)
                scope.launch { snackbarHostState.showSnackbar("Nextcloud-instellingen opgeslagen") }
            }
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun VolvoLinkSection(isLinked: Boolean, canLink: Boolean, onLink: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            Button(
                onClick = onLink,
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
        }
    }
}

@Composable
private fun NextcloudSection(
    url: String,
    username: String,
    password: String,
    onUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nextcloud synchronisatie", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "De volledige rittenlijst wordt automatisch als CSV geüpload na elke rit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Nextcloud URL") },
                placeholder = { Text("https://cloud.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Gebruikersnaam") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("App-wachtwoord") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Nextcloud opslaan")
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    title: String,
    desc: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
