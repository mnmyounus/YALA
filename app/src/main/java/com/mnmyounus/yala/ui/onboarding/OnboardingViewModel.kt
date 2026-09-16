package com.mnmyounus.yala.ui.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.mnmyounus.yala.data.local.SecurePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    val cameraGranted: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val recoveryKey: String? = null,
    val lockSetupOpen: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SecurePrefs
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun next() {
        val target = (_state.value.step + 1).coerceAtMost(TOTAL_STEPS - 1)
        // The recovery key is generated exactly once, when the user reaches that step.
        val key = if (target == RECOVERY_STEP && _state.value.recoveryKey == null) {
            prefs.createRecoveryKey()
        } else _state.value.recoveryKey
        _state.update { it.copy(step = target, recoveryKey = key) }
    }

    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun setCameraGranted(granted: Boolean) = _state.update { it.copy(cameraGranted = granted) }

    fun acceptDisclaimer() {
        prefs.clearRecoveryPlaintext()
        _state.update { it.copy(disclaimerAccepted = true) }
    }

    fun openLockSetup() = _state.update { it.copy(lockSetupOpen = true) }

    fun copyKey(context: Context) {
        val key = _state.value.recoveryKey ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("YALA recovery key", key))
        Toast.makeText(context, "Recovery key copied", Toast.LENGTH_SHORT).show()
    }

    fun saveKey(context: Context) {
        val key = _state.value.recoveryKey ?: return
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "yala-recovery-key.txt")
        file.writeText("YALA recovery key\n$key\nKeep this somewhere safe and offline.\n")
        Toast.makeText(context, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }

    companion object {
        const val TOTAL_STEPS = 8
        const val RECOVERY_STEP = 6
    }
}
