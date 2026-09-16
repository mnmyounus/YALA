package com.mnmyounus.yala.ui.onboarding

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mnmyounus.yala.data.service.YalaDeviceAdminReceiver

/**
 * Step-by-step walkthrough: welcome, permissions, credential setup,
 * recovery key, feature tour and the offline privacy disclaimer.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setCameraGranted(granted) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LinearProgressIndicator(
            progress = { (state.step + 1) / OnboardingViewModel.TOTAL_STEPS.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Step ${state.step + 1} of ${OnboardingViewModel.TOTAL_STEPS}",
            style = MaterialTheme.typography.bodySmall)

        when (state.step) {
            0 -> StepCard(
                title = "Welcome to YALA",
                body = "Your App Lock and Analyzer keeps your apps private with your own PIN, password, pattern or secret image order — and quietly photographs anyone who tries to get in.\n\nBuilt by MNM YOUNUS."
            )

            1 -> StepCard(
                title = "Turn on app detection",
                body = "YALA needs the accessibility service so it can notice the moment a protected app opens and show the lock instantly."
            ) {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Open accessibility settings") }
            }

            2 -> StepCard(
                title = "Allow drawing over apps",
                body = "The lock screen appears on top of the app being opened. This needs the display-over-other-apps permission."
            ) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }) { Text("Allow overlay") }
            }

            3 -> StepCard(
                title = "Stop intruders removing YALA",
                body = "Device admin blocks uninstalling and force-stopping YALA. You can switch it off any time from Settings."
            ) {
                Button(onClick = { requestDeviceAdmin(context) }) { Text("Enable protection") }
            }

            4 -> StepCard(
                title = "Camera for intruder photos",
                body = "The front camera takes a silent photo on unlock attempts. Photos stay on this device in YALA's own gallery."
            ) {
                Button(onClick = { cameraLauncher.launch(android.Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            }

            5 -> StepCard(
                title = "Choose your lock",
                body = "Pick a 4-digit PIN, an 8-character password, a pattern, or a secret order of your own images. You can change this later and even set a different lock per app."
            ) {
                Button(onClick = { viewModel.openLockSetup() }) { Text("Set up my lock") }
            }

            6 -> StepCard(
                title = "Save your recovery key",
                body = "This 12-character key is the only way back in if you forget your lock. Copy it or save it as a file now — it is shown once."
            ) {
                Text(
                    state.recoveryKey ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.copyKey(context) }) { Text("Copy") }
                    OutlinedButton(onClick = { viewModel.saveKey(context) }) { Text("Save as file") }
                }
            }

            7 -> StepCard(
                title = "Privacy and disclaimer",
                body = "YALA has no internet permission, no ads and no accounts. Locks, photos and your recovery key never leave this device. Uninstalling YALA deletes all of it permanently.\n\nUse YALA only on devices you own or are authorised to protect."
            ) {
                Button(onClick = { viewModel.acceptDisclaimer() }) {
                    Text(if (state.disclaimerAccepted) "Accepted" else "I understand and accept")
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { viewModel.back() }, enabled = state.step > 0) { Text("Back") }
            Button(
                onClick = {
                    if (state.step == OnboardingViewModel.TOTAL_STEPS - 1) onFinished()
                    else viewModel.next()
                },
                enabled = state.step != OnboardingViewModel.TOTAL_STEPS - 1 || state.disclaimerAccepted
            ) {
                Text(if (state.step == OnboardingViewModel.TOTAL_STEPS - 1) "Finish" else "Next")
            }
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    body: String,
    content: @Composable (() -> Unit)? = null
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            content?.invoke()
        }
    }
}

private fun requestDeviceAdmin(context: Context) {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val component = YalaDeviceAdminReceiver.component(context)
    if (!dpm.isAdminActive(component)) {
        context.startActivity(
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                .putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Device admin stops an intruder from uninstalling or force-stopping YALA."
                )
        )
    }
}
