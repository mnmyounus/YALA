package com.mnmyounus.yala.domain.repository

import com.mnmyounus.yala.domain.model.AppInfo
import com.mnmyounus.yala.domain.model.CaptureOutcome
import com.mnmyounus.yala.domain.model.GlobalLockConfig
import com.mnmyounus.yala.domain.model.IntruderShot
import com.mnmyounus.yala.domain.model.LockMode
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.model.LockedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LockRepository {
    val lockedApps: StateFlow<List<LockedApp>>

    fun installedApps(includeSystem: Boolean = true): List<AppInfo>
    fun isProtected(packageName: String): Boolean
    fun lockTypeFor(packageName: String): LockType
    fun hintFor(packageName: String): String
    fun setLocked(packageName: String, locked: Boolean)
    fun updateApp(app: LockedApp)

    fun lockMode(): LockMode
    fun setLockMode(mode: LockMode)
    fun globalConfig(): GlobalLockConfig
    fun setGlobalConfig(config: GlobalLockConfig)

    fun grantSession(packageName: String)
    fun hasActiveSession(packageName: String): Boolean
    fun clearSessionIfLeft(currentForeground: String)
    fun clearAllSessions()
}

interface IntruderRepository {
    fun shots(outcome: CaptureOutcome): Flow<List<IntruderShot>>
    suspend fun capture(packageName: String, outcome: CaptureOutcome)
    suspend fun delete(id: Long)
    suspend fun clear(outcome: CaptureOutcome)
}
