package com.mnmyounus.yala.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.mnmyounus.yala.data.local.SecurePrefs
import com.mnmyounus.yala.domain.model.GlobalLockConfig
import com.mnmyounus.yala.domain.model.LockMode
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.model.ThemeMode
import com.mnmyounus.yala.domain.repository.LockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lockMode: LockMode = LockMode.GLOBAL,
    val globalLockType: LockType = LockType.PIN,
    val globalHint: String = "",
    val captureOnSuccess: Boolean = true,
    val captureOnFailure: Boolean = true,
    val recoveryKeyPreview: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SecurePrefs,
    private val lockRepository: LockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            themeMode = prefs.themeMode,
            lockMode = prefs.lockMode,
            globalLockType = prefs.globalConfig.lockType,
            globalHint = prefs.globalConfig.hint,
            captureOnSuccess = prefs.captureOnSuccess,
            captureOnFailure = prefs.captureOnFailure
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.themeMode = mode
        _state.update { it.copy(themeMode = mode) }
    }

    fun setLockMode(mode: LockMode) {
        lockRepository.setLockMode(mode)
        _state.update { it.copy(lockMode = mode) }
    }

    fun setGlobalLockType(type: LockType) {
        lockRepository.setGlobalConfig(GlobalLockConfig(type, _state.value.globalHint))
        _state.update { it.copy(globalLockType = type) }
    }

    fun setGlobalHint(hint: String) {
        lockRepository.setGlobalConfig(GlobalLockConfig(_state.value.globalLockType, hint))
        _state.update { it.copy(globalHint = hint) }
    }

    fun setCaptureSuccess(enabled: Boolean) {
        prefs.captureOnSuccess = enabled
        _state.update { it.copy(captureOnSuccess = enabled) }
    }

    fun setCaptureFailure(enabled: Boolean) {
        prefs.captureOnFailure = enabled
        _state.update { it.copy(captureOnFailure = enabled) }
    }

    fun regenerateRecoveryKey() {
        val key = prefs.createRecoveryKey()
        _state.update { it.copy(recoveryKeyPreview = key) }
    }

    fun copyRecoveryKey(context: Context) {
        val key = _state.value.recoveryKeyPreview ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("YALA recovery key", key))
        Toast.makeText(context, "Recovery key copied", Toast.LENGTH_SHORT).show()
    }

    /** Saves to app-private external files dir; nothing is uploaded anywhere. */
    fun saveRecoveryKeyToFile(context: Context) {
        val key = _state.value.recoveryKeyPreview ?: return
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "yala-recovery-key.txt")
        file.writeText("YALA recovery key\n$key\nKeep this somewhere safe and offline.\n")
        Toast.makeText(context, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
