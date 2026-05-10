package com.rykle.kmregistratie.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.rykle.kmregistratie.R
import com.rykle.kmregistratie.data.local.preferences.AppPreferences
import com.rykle.kmregistratie.data.remote.VolvoAuthService
import com.rykle.kmregistratie.ui.theme.KmRegistratieTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private sealed interface OAuthState {
    data object Browsing : OAuthState
    data object Loading : OAuthState
    data object Success : OAuthState
    data class Error(val message: String) : OAuthState
}

@AndroidEntryPoint
class VolvoOAuthActivity : ComponentActivity() {

    @Inject lateinit var authService: VolvoAuthService
    @Inject lateinit var prefs: AppPreferences

    private var uiState by mutableStateOf<OAuthState>(OAuthState.Browsing)
    private var callbackUrl by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val verifier = VolvoPkce.generateVerifier()
        prefs.pendingCodeVerifier = verifier
        val challenge = VolvoPkce.generateChallenge(verifier)

        val authUri = Uri.Builder()
            .scheme("https")
            .authority("volvoid.eu.volvocars.com")
            .path("/as/authorization.oauth2")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", prefs.clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        startActivity(Intent(Intent.ACTION_VIEW, authUri))

        setContent {
            KmRegistratieTheme {
                OAuthScreen(
                    state = uiState,
                    callbackUrl = callbackUrl,
                    onCallbackUrlChange = { callbackUrl = it },
                    onConfirm = ::completeAuth,
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun completeAuth() {
        val code = runCatching { Uri.parse(callbackUrl).getQueryParameter("code") }
            .getOrNull()
        if (code.isNullOrBlank()) {
            uiState = OAuthState.Error(getString(R.string.oauth_error_no_code))
            return
        }
        val verifier = prefs.pendingCodeVerifier
        if (verifier.isBlank()) {
            uiState = OAuthState.Error(getString(R.string.oauth_error_verifier))
            return
        }
        uiState = OAuthState.Loading
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    authService.exchangeCode(
                        grantType = "authorization_code",
                        code = code,
                        redirectUri = REDIRECT_URI,
                        clientId = prefs.clientId,
                        clientSecret = prefs.clientSecret,
                        codeVerifier = verifier,
                    )
                }
                prefs.pendingCodeVerifier = ""
                prefs.accessToken = response.accessToken
                if (!response.refreshToken.isNullOrBlank()) {
                    prefs.refreshToken = response.refreshToken
                }
                uiState = OAuthState.Success
                delay(1500)
                finish()
            } catch (e: Exception) {
                prefs.pendingCodeVerifier = ""
                uiState = OAuthState.Error(e.message ?: getString(R.string.oauth_error_unknown))
            }
        }
    }

    companion object {
        const val REDIRECT_URI = "https://localhost"
        const val SCOPE = "openid conve:odometer_status"
    }
}

@Composable
private fun OAuthScreen(
    state: OAuthState,
    callbackUrl: String,
    onCallbackUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            when (state) {
                OAuthState.Browsing -> Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.oauth_step_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.oauth_step_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = callbackUrl,
                        onValueChange = onCallbackUrlChange,
                        label = { Text(stringResource(R.string.oauth_url_label)) },
                        placeholder = { Text("https://localhost?code=...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                    )
                    Button(
                        onClick = onConfirm,
                        enabled = callbackUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.oauth_confirm))
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.oauth_dismiss))
                    }
                }

                OAuthState.Loading -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.oauth_loading))
                }

                OAuthState.Success -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("✓", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.oauth_success), style = MaterialTheme.typography.titleLarge)
                }

                is OAuthState.Error -> Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(R.string.oauth_error_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onDismiss) { Text(stringResource(R.string.oauth_dismiss)) }
                }
            }
        }
    }
}
