package com.mnmyounus.yala.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mnmyounus.yala.data.service.YalaDeviceAdminReceiver
import com.mnmyounus.yala.domain.model.LockMode
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeChanged: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setTheme(mode); onThemeChanged(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Divider()
            Text("Locking mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LockMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.lockMode == mode,
                        onClick = { viewModel.setLockMode(mode) },
                        label = { Text(if (mode == LockMode.GLOBAL) "Global" else "Per app") }
                    )
                }
            }
            Text(
                if (state.lockMode == LockMode.GLOBAL)
                    "One lock type and hint applies to every protected app."
                else
                    "Each app keeps its own lock type and hint.",
                style = MaterialTheme.typography.bodySmall
            )

            if (state.lockMode == LockMode.GLOBAL) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockType.entries.forEach { type ->
                        FilterChip(
                            selected = state.globalLockType == type,
                            onClick = { viewModel.setGlobalLockType(type) },
                            label = { Text(type.name.replace('_', ' ')) }
                        )
                    }
                }
                OutlinedTextField(
                    value = state.globalHint,
                    onValueChange = viewModel::setGlobalHint,
                    label = { Text("Global hint") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Divider()
            Text("Intruder capture", style = MaterialTheme.typography.titleMedium)
            ToggleRow("Photo on successful unlock", state.captureOnSuccess, viewModel::setCaptureSuccess)
            ToggleRow("Photo on failed attempt", state.captureOnFailure, viewModel::setCaptureFailure)

            Divider()
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open accessibility settings") }
            Button(
                onClick = {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val component = YalaDeviceAdminReceiver.component(context)
                    if (!dpm.isAdminActive(component)) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                            .putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Device admin stops an intruder from uninstalling or force-stopping YALA."
                            )
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enable uninstall protection") }

            Divider()
            Text("Recovery", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { viewModel.regenerateRecoveryKey() }, modifier = Modifier.fillMaxWidth()) {
                Text("Generate a new recovery key")
            }
            state.recoveryKeyPreview?.let { key ->
                Text("Your key: $key", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.copyRecoveryKey(context) }) { Text("Copy") }
                    Button(onClick = { viewModel.saveRecoveryKeyToFile(context) }) { Text("Save as file") }
                }
            }

            Divider()
            Text(
                "YALA works entirely offline. It has no internet permission, shows no ads, and never sends your data anywhere.",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Developed by MNM YOUNUS", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
