package com.ste03.smartscan.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ste03.smartscan.lock.LockManager
import com.ste03.smartscan.ui.theme.Green

@Composable
fun LockScreen(lockManager: LockManager, onUnlocked: () -> Unit) {
    var passcode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val activity = LocalContext.current as FragmentActivity

    // Biometric prompt (if enabled)
    val biometricPrompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onUnlocked()
                }
            }
        )
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Smart Scan")
            .setSubtitle("Use your fingerprint or face")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    fun tryBiometric() {
        if (lockManager.biometricEnabled()) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(Unit) { tryBiometric() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Green
            )
            Spacer(Modifier.height(16.dp))
            Text("Smart Scan", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Enter your passcode to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = passcode,
                onValueChange = {
                    passcode = it
                    error = false
                },
                label = { Text("Passcode") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error
            )
            if (error) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Incorrect passcode",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (lockManager.verify(passcode)) {
                        onUnlocked()
                    } else {
                        error = true
                        passcode = ""
                    }
                },
                enabled = passcode.isNotEmpty()
            ) {
                Text("Unlock")
            }
            if (lockManager.biometricEnabled()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { tryBiometric() }) {
                    Text("Use fingerprint / face")
                }
            }
        }
    }
}
