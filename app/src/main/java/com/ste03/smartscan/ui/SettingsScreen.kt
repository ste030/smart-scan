package com.ste03.smartscan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ste03.smartscan.ui.theme.Themes
import com.ste03.smartscan.vm.AppViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenTheme: () -> Unit) {
    val lockManager = viewModel.lockManager
    val themeName by viewModel.themeName.collectAsStateWithLifecycle()
    var lockEnabled by remember { mutableStateOf(lockManager.isLockEnabled()) }
    var biometricEnabled by remember { mutableStateOf(lockManager.biometricEnabled()) }

    var settingPasscode by remember { mutableStateOf(false) }
    var clearingLock by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "App lock",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (lockEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Passcode enabled", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { clearingLock = true }) { Text("Disable") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Unlock with fingerprint / face", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { enabled ->
                            biometricEnabled = enabled
                            lockManager.setBiometricEnabled(enabled)
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Passcode lock", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { settingPasscode = true }) { Text("Set") }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenTheme)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Palette, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Themes.colorFor(themeName))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    themeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text("Smart Scan 1.6.0", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Scan documents, organise them into folders, and export as PDF or images.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (settingPasscode) {
        SetPasscodeDialog(
            onDismiss = { settingPasscode = false },
            onConfirm = { passcode ->
                lockManager.setPasscode(passcode)
                lockEnabled = true
                settingPasscode = false
            }
        )
    }

    if (clearingLock) {
        AlertDialog(
            onDismissRequest = { clearingLock = false },
            title = { Text("Disable passcode lock?") },
            text = { Text("Your app will no longer ask for a passcode when opened.") },
            confirmButton = {
                Button(onClick = {
                    lockManager.clearLock()
                    lockEnabled = false
                    biometricEnabled = false
                    clearingLock = false
                }) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { clearingLock = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SetPasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && confirm != passcode
    val tooShort = passcode.isNotEmpty() && passcode.length < 4

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set passcode") },
        text = {
            Column {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode (4+ digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = mismatch
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(passcode) },
                enabled = passcode.length >= 4 && confirm == passcode
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
