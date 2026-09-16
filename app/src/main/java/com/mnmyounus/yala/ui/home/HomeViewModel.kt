package com.mnmyounus.yala.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnmyounus.yala.domain.model.AppInfo
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.repository.LockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppRow(val info: AppInfo, val locked: Boolean, val lockType: LockType)

data class HomeUiState(
    val apps: List<AppRow> = emptyList(),
    val showSystemApps: Boolean = true,
    val query: String = "",
    val lockedCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun toggleSystemApps() {
        _state.update { it.copy(showSystemApps = !it.showSystemApps) }
        refresh()
    }

    fun toggle(packageName: String, locked: Boolean) {
        repository.setLocked(packageName, locked)
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        val showSystem = _state.value.showSystemApps
        val rows = withContext(Dispatchers.IO) {
            repository.installedApps(includeSystem = showSystem).map { info ->
                AppRow(
                    info = info,
                    locked = repository.isProtected(info.packageName),
                    lockType = repository.lockTypeFor(info.packageName)
                )
            }
        }
        _state.update { it.copy(apps = rows, lockedCount = rows.count { r -> r.locked }) }
    }
}
