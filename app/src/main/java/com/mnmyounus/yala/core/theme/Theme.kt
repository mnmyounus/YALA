package com.mnmyounus.yala.core.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.mnmyounus.yala.domain.model.ThemeMode

private val Teal = Color(0xFF0FB5A3)
private val TealDark = Color(0xFF0A8578)
private val Amber = Color(0xFFFFB020)

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    secondary = Amber,
    background = Color(0xFFF6F8F8),
    surface = Color.White,
    onSurface = Color(0xFF10201F),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF00201C),
    secondary = Amber,
    background = Color(0xFF0B1211),
    surface = Color(0xFF121A19),
    onSurface = Color(0xFFE3E8E7),
    error = Color(0xFFF2B8B5)
)

/** True when running on Android TV; drives larger type and focus-first layouts. */
val LocalIsTv = staticCompositionLocalOf { false }

fun Context.isTvDevice(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature("android.software.leanback")
}

@Composable
fun YalaTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val isTv = context.isTvDevice()

    val base = Typography()
    val typography = if (isTv) {
        base.copy(
            bodyLarge = base.bodyLarge.copy(fontSize = 20.sp),
            titleLarge = base.titleLarge.copy(fontSize = 30.sp),
            labelLarge = TextStyle(fontSize = 20.sp)
        )
    } else base

    CompositionLocalProvider(LocalIsTv provides isTv) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = typography,
            content = content
        )
    }
}
