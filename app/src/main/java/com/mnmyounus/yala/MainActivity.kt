package com.mnmyounus.yala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mnmyounus.yala.core.theme.YalaTheme
import com.mnmyounus.yala.data.local.SecurePrefs
import com.mnmyounus.yala.domain.model.ThemeMode
import com.mnmyounus.yala.ui.gallery.GalleryScreen
import com.mnmyounus.yala.ui.home.HomeScreen
import com.mnmyounus.yala.ui.onboarding.OnboardingScreen
import com.mnmyounus.yala.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: SecurePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(prefs.themeMode) }
            YalaTheme(theme) {
                val nav = rememberNavController()
                val start = if (prefs.onboardingCompleted) "home" else "onboarding"
                NavHost(navController = nav, startDestination = start) {
                    composable("onboarding") {
                        OnboardingScreen(onFinished = {
                            prefs.onboardingCompleted = true
                            nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                        })
                    }
                    composable("home") {
                        HomeScreen(
                            onOpenSettings = { nav.navigate("settings") },
                            onOpenGallery = { nav.navigate("gallery") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onThemeChanged = { mode: ThemeMode ->
                                prefs.themeMode = mode
                                theme = mode
                            },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("gallery") { GalleryScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
