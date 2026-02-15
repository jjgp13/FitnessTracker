package com.healthsync.ai.ui.screen.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val WEB_CLIENT_ID = "768280432769-8cbh30b50v48a8c69j2psph9ml065lkq.apps.googleusercontent.com"

@Composable
fun AuthScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMorningBriefing: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigationTarget by viewModel.navigationTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(navigationTarget) {
        when (navigationTarget) {
            AuthViewModel.NavigationTarget.ONBOARDING -> {
                viewModel.onNavigationHandled()
                onNavigateToOnboarding()
            }
            AuthViewModel.NavigationTarget.MORNING_BRIEFING -> {
                viewModel.onNavigationHandled()
                onNavigateToMorningBriefing()
            }
            null -> {}
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { viewModel.signInWithGoogle(it) }
                    ?: viewModel.onSignInError("No ID token received")
            } catch (e: ApiException) {
                viewModel.onSignInError("Google Sign-In failed: ${e.statusCode} - ${e.message}")
            }
        } else {
            viewModel.onSignInError("Sign-in cancelled (resultCode: ${result.resultCode})")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "HealthSync AI",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your AI-powered fitness companion",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (uiState.status) {
            AuthStatus.LOADING -> {
                CircularProgressIndicator()
            }
            else -> {
                Button(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        launcher.launch(client.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
            }
        }

        if (uiState.status == AuthStatus.ERROR) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.errorMessage ?: "An error occurred",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
