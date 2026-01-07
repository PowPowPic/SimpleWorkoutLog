package com.poweder.simpleworkoutlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.poweder.simpleworkoutlog.ui.navigation.AppNavHost
import com.poweder.simpleworkoutlog.ui.navigation.BottomNavBar
import com.poweder.simpleworkoutlog.ui.navigation.Screen
import com.poweder.simpleworkoutlog.ui.theme.SimpleWorkoutLogTheme
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel

@Composable
fun SimpleWorkoutLogApp(viewModel: WorkoutViewModel) {
    SimpleWorkoutLogTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // ナビバーを表示する画面
        val showBottomNav = currentRoute in listOf(
            Screen.Home.route,
            Screen.History.route,
            Screen.Calendar.route,
            Screen.Graph.route,
            Screen.Settings.route
        )

        // 全画面グラデーション（左→右：ダーク→シルバー）
        val backgroundGradient = Brush.horizontalGradient(
            colors = listOf(
                WorkoutColors.BackgroundDark,
                WorkoutColors.BackgroundMedium
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                bottomBar = {
                    if (showBottomNav) {
                        BottomNavBar(navController = navController)
                    }
                }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}