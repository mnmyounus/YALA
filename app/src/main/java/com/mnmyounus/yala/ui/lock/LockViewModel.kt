package com.mnmyounus.yala.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnmyounus.yala.core.util.CryptoUtils
import com.mnmyounus.yala.data.local.SecurePrefs
import com.mnmyounus.yala.domain.model.CaptureOutcome
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.repository.IntruderRepository
import com.mnmyounus.yala.domain.repository.LockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockUiState(
    val packageName: String = "",
    val lockType: LockType = LockType.PIN,
    val hint: String = "",
    val input: String = "",
    val failedAttempts: Int = 0,
    val errorMessage: String? = null,
    val recoveryVisible: Boolean = false,
    val unlocked: Boolean = false,
    val imagePool: List<String> = emptyList(),
    val selectedImages: List<String> = emptyList()
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val lockRepository: LockRepository,
    private val intruderRepository: IntruderRepository,
    private val prefs: SecurePrefs
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    fun bind(packageName: String) {
        _state.update {
            it.copy(
                packageName = packageName,
                lockType = lockRepository.lockTypeFor(packageName),
                hint = lockRepository.hintFor(packageName),
                imagePool = prefs.imagePoolIds
            )
        }
    }

    fun onInputChanged(value: String) = _state.update { it.copy(input = value, errorMessage = null) }

    fun onImageTapped(imageId: String) = _state.update {
        val next = if (it.selectedImages.size >= 4) listOf(imageId) else it.selectedImages + imageId
        it.copy(selectedImages = next, errorMessage = null)
    }

    fun submitPin(pin: String) = submit(LockType.PIN, pin)

    fun submitPassword(password: String) = submit(LockType.PASSWORD, password)

    fun submitPattern(dots: List<Int>) = submit(LockType.PATTERN, CryptoUtils.encodePattern(dots))

    fun submitImageSequence() =
        submit(LockType.IMAGE_SEQUENCE, CryptoUtils.encodeImageSequence(_state.value.selectedImages))

    fun submitRecoveryKey(key: String) {
        if (prefs.verifyRecoveryKey(key)) {
            grantAccess(bypass = true)
        } else {
            _state.update { it.copy(errorMessage = "That recovery key doesn't match.") }
        }
    }

    fun toggleRecovery() = _state.update { it.copy(recoveryVisible = !it.recoveryVisible) }

    private fun submit(type: LockType, secret: String) {
        if (prefs.verify(type, secret)) grantAccess() else denyAccess()
    }

    private fun grantAccess(bypass: Boolean = false) {
        val pkg = _state.value.packageName
        lockRepository.grantSession(pkg)
        if (prefs.captureOnSuccess && !bypass) {
            viewModelScope.launch { intruderRepository.capture(pkg, CaptureOutcome.SUCCESS) }
        }
        _state.update { it.copy(unlocked = true, input = "", selectedImages = emptyList()) }
    }

    private fun denyAccess() {
        val pkg = _state.value.packageName
        if (prefs.captureOnFailure) {
            viewModelScope.launch { intruderRepository.capture(pkg, CaptureOutcome.FAILURE) }
        }
        _state.update {
            it.copy(
                failedAttempts = it.failedAttempts + 1,
                input = "",
                selectedImages = emptyList(),
                errorMessage = "Incorrect. Try again."
            )
        }
    }
}
