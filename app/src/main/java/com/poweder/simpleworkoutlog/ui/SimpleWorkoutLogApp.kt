package com.poweder.simpleworkoutlog.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.poweder.simpleworkoutlog.ui.navigation.AppNavHost
import com.poweder.simpleworkoutlog.ui.navigation.BottomNavBar
import com.poweder.simpleworkoutlog.ui.navigation.Screen
import com.poweder.simpleworkoutlog.ui.theme.SimpleWorkoutLogTheme
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel

@Composable
fun SimpleWorkoutLogApp(
    viewModel: WorkoutViewModel
) {
    SimpleWorkoutLogTheme {
        val navController = rememberNavController()
        
        Scaffold(
            bottomBar = { BottomNavBar(navController = navController) }
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
