package com.poweder.simpleworkoutlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.poweder.simpleworkoutlog.R

sealed class Screen(
    val route: String,
    val resourceId: Int = 0,
    val icon: ImageVector = Icons.Default.Home
) {
    companion object {
        val bottomNavItems: List<Screen> by lazy {
            listOf(Home, History, Calendar, Graph, Settings)
        }
    }

    // ナビバーに表示する画面
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object History : Screen("history", R.string.nav_history, Icons.Default.List)
    object Calendar : Screen("calendar", R.string.nav_calendar, Icons.Default.DateRange)
    object Graph : Screen("graph", R.string.nav_graph, Icons.Default.ShowChart)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    // ナビバーに表示しない画面（sessionId付きで編集モード対応）
    object Strength : Screen("strength/{sessionId}") {
        fun createRoute(sessionId: Long? = null) = "strength/${sessionId ?: -1}"
    }
    object Cardio : Screen("cardio/{sessionId}") {
        fun createRoute(sessionId: Long? = null) = "cardio/${sessionId ?: -1}"
    }
    object Interval : Screen("interval")
    object Studio : Screen("studio/{sessionId}") {
        fun createRoute(sessionId: Long? = null) = "studio/${sessionId ?: -1}"
    }
    object Other : Screen("other/{sessionId}") {
        fun createRoute(sessionId: Long? = null) = "other/${sessionId ?: -1}"
    }
}