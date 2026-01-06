package com.poweder.simpleworkoutlog.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.poweder.simpleworkoutlog.R

sealed class Screen(
    val route: String,
    @StringRes val resourceId: Int,
    val icon: ImageVector
) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Calendar : Screen("calendar", R.string.nav_calendar, Icons.Default.CalendarMonth)
    object Graph : Screen("graph", R.string.nav_graph, Icons.Default.ShowChart)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    
    // 筋トレ画面
    object StrengthTraining : Screen("strength_training", R.string.workout_strength, Icons.Default.Home)
    
    // 有酸素画面
    object Cardio : Screen("cardio", R.string.workout_cardio, Icons.Default.Home)
    
    // インターバル画面
    object Interval : Screen("interval", R.string.workout_interval, Icons.Default.Home)
    
    // スタジオ画面
    object Studio : Screen("studio", R.string.workout_studio, Icons.Default.Home)
    
    companion object {
        val bottomNavItems = listOf(Home, History, Calendar, Graph, Settings)
    }
}
