package com.rykle.kmregistratie.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
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

        val canLink = clientId.isNotBlank() && clientSecret.isNotBlank()
        Button(
            onClick = {
                viewModel.saveSettings(vin, apiKey, clientId, clientSecret)
                context.startActivity(Intent(context, VolvoOAuthActivity::class.java))
            },
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
