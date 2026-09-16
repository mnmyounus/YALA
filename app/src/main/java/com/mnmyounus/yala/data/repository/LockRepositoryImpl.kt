package com.mnmyounus.yala.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.mnmyounus.yala.data.local.SecurePrefs
import com.mnmyounus.yala.domain.model.AppInfo
import com.mnmyounus.yala.domain.model.GlobalLockConfig
import com.mnmyounus.yala.domain.model.LockMode
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.model.LockedApp
import com.mnmyounus.yala.domain.repository.LockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockRepositoryImpl @Inject constructor(
    private val context: Context,
    private val prefs: SecurePrefs
) : LockRepository {

    private val _lockedApps = MutableStateFlow(prefs.lockedApps())
    override val lockedApps: StateFlow<List<LockedApp>> = _lockedApps

    /** packageName -> unlock expiry timestamp. An unlocked app stays open until it leaves foreground. */
    private val sessions = mutableMapOf<String, Long>()

    override fun installedApps(includeSystem: Boolean): List<AppInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { includeSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystemApp = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override fun isProtected(packageName: String): Boolean =
        _lockedApps.value.any { it.packageName == packageName && it.enabled }

    override fun lockTypeFor(packageName: String): LockType = when (prefs.lockMode) {
        LockMode.GLOBAL -> prefs.globalConfig.lockType
        LockMode.INDIVIDUAL ->
            _lockedApps.value.firstOrNull { it.packageName == packageName }?.lockType
                ?: prefs.globalConfig.lockType
    }

    override fun hintFor(packageName: String): String = when (prefs.lockMode) {
        LockMode.GLOBAL -> prefs.globalConfig.hint
        LockMode.INDIVIDUAL ->
            _lockedApps.value.firstOrNull { it.packageName == packageName }?.hint.orEmpty()
    }

    override fun setLocked(packageName: String, locked: Boolean) {
        val current = _lockedApps.value.toMutableList()
        val idx = current.indexOfFirst { it.packageName == packageName }
        if (locked) {
            if (idx == -1) current += LockedApp(packageName, prefs.globalConfig.lockType)
            else current[idx] = current[idx].copy(enabled = true)
        } else if (idx != -1) {
            current.removeAt(idx)
        }
        persist(current)
    }

    override fun updateApp(app: LockedApp) {
        val current = _lockedApps.value.toMutableList()
        val idx = current.indexOfFirst { it.packageName == app.packageName }
        if (idx == -1) current += app else current[idx] = app
        persist(current)
    }

    override fun lockMode(): LockMode = prefs.lockMode

    override fun setLockMode(mode: LockMode) { prefs.lockMode = mode }

    override fun globalConfig(): GlobalLockConfig = prefs.globalConfig

    override fun setGlobalConfig(config: GlobalLockConfig) { prefs.globalConfig = config }

    // ---- unlock sessions ---------------------------------------------------

    override fun grantSession(packageName: String) {
        sessions[packageName] = System.currentTimeMillis()
    }

    override fun hasActiveSession(packageName: String): Boolean = sessions.containsKey(packageName)

    override fun clearSessionIfLeft(currentForeground: String) {
        sessions.keys.filter { it != currentForeground }.forEach { sessions.remove(it) }
    }

    override fun clearAllSessions() = sessions.clear()

    private fun persist(list: List<LockedApp>) {
        prefs.saveLockedApps(list)
        _lockedApps.value = list
    }
}
