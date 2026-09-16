package com.mnmyounus.yala.ui.lock

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.mnmyounus.yala.core.theme.YalaTheme
import com.mnmyounus.yala.data.local.SecurePrefs
import com.mnmyounus.yala.data.repository.IntruderRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The overlay shown on top of a protected app. It cannot be dismissed with Back,
 * and screenshots are blocked while it is visible.
 */
@AndroidEntryPoint
class LockScreenActivity : ComponentActivity() {

    @Inject lateinit var prefs: SecurePrefs
    @Inject lateinit var intruderRepositoryImpl: IntruderRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // CameraX binds to this activity so intruder shots can be taken silently.
        intruderRepositoryImpl.lifecycleOwner = this

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()

        setContent {
            YalaTheme(prefs.themeMode) {
                val vm: LockViewModel = hiltViewModel()
                val state by vm.state.collectAsState()

                LaunchedEffect(targetPackage) { vm.bind(targetPackage) }
                LaunchedEffect(state.unlocked) { if (state.unlocked) finish() }

                // Back must not bypass the lock: send the intruder to the home screen.
                BackHandler { goHome() }

                LockScreen(state = state, viewModel = vm)
            }
        }
    }

    override fun onDestroy() {
        if (intruderRepositoryImpl.lifecycleOwner === this) intruderRepositoryImpl.lifecycleOwner = null
        super.onDestroy()
    }

    private fun goHome() {
        val home = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
    }
}
