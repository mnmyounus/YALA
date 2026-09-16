package com.mnmyounus.yala.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnmyounus.yala.domain.model.CaptureOutcome
import com.mnmyounus.yala.domain.model.IntruderShot
import com.mnmyounus.yala.domain.repository.IntruderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val tab: Int = 0,
    val successShots: List<IntruderShot> = emptyList(),
    val failureShots: List<IntruderShot> = emptyList()
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: IntruderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.shots(CaptureOutcome.SUCCESS).collect { list ->
                _state.update { it.copy(successShots = list) }
            }
        }
        viewModelScope.launch {
            repository.shots(CaptureOutcome.FAILURE).collect { list ->
                _state.update { it.copy(failureShots = list) }
            }
        }
    }

    fun selectTab(index: Int) = _state.update { it.copy(tab = index) }

    fun clearCurrentTab() = viewModelScope.launch {
        repository.clear(if (_state.value.tab == 0) CaptureOutcome.SUCCESS else CaptureOutcome.FAILURE)
    }
}
