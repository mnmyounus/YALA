package com.mnmyounus.yala.data.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.mnmyounus.yala.domain.repository.LockRepository
import com.mnmyounus.yala.ui.lock.LockScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Detects foreground app changes and instantly raises the lock overlay for
 * protected packages (user-installed AND system apps such as Settings or the
 * package installer).
 */
@AndroidEntryPoint
class YalaAccessibilityService : AccessibilityService() {

    @Inject lateinit var lockRepository: LockRepository

    private var lastPackage: String? = null
    private var lastEventTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        startService(Intent(this, LockForegroundService::class.java))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val now = System.currentTimeMillis()
        if (pkg == lastPackage && now - lastEventTime < 400) return
        lastPackage = pkg
        lastEventTime = now

        if (!lockRepository.isProtected(pkg)) {
            lockRepository.clearSessionIfLeft(pkg)
            return
        }
        if (lockRepository.hasActiveSession(pkg)) return

        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(LockScreenActivity.EXTRA_PACKAGE, pkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
