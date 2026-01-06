package com.poweder.simpleworkoutlog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WorkoutColors.AccentOrange,
    secondary = WorkoutColors.AccentOrangeLight,
    tertiary = WorkoutColors.StrengthCardStart,
    background = WorkoutColors.BackgroundDark,
    surface = WorkoutColors.DialogBackground,
    onPrimary = WorkoutColors.TextPrimary,
    onSecondary = WorkoutColors.TextPrimary,
    onTertiary = WorkoutColors.TextPrimary,
    onBackground = WorkoutColors.TextPrimary,
    onSurface = WorkoutColors.TextPrimary
)

@Composable
fun SimpleWorkoutLogTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WorkoutColors.BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
