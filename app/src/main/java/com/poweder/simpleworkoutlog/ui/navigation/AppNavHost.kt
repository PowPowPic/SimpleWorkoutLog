package com.poweder.simpleworkoutlog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.poweder.simpleworkoutlog.ui.cardio.CardioScreen
import com.poweder.simpleworkoutlog.ui.interval.IntervalScreen
import com.poweder.simpleworkoutlog.ui.other.OtherScreen
import com.poweder.simpleworkoutlog.ui.screen.CalendarScreen
import com.poweder.simpleworkoutlog.ui.screen.GraphScreen
import com.poweder.simpleworkoutlog.ui.screen.HistoryScreen
import com.poweder.simpleworkoutlog.ui.screen.MainScreen
import com.poweder.simpleworkoutlog.ui.settings.SettingsScreen
import com.poweder.simpleworkoutlog.ui.strength.StrengthTrainingScreen
import com.poweder.simpleworkoutlog.ui.studio.StudioScreen
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import java.time.LocalDate

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: WorkoutViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            MainScreen(
                viewModel = viewModel,
                onSettingsClick = onSettingsClick,
                onNavigateToStrength = {
                    navController.navigate(Screen.Strength.route)
                },
                onNavigateToCardio = {
                    navController.navigate(Screen.Cardio.route)
                },
                onNavigateToInterval = {
                    navController.navigate(Screen.Interval.route)
                },
                onNavigateToStudio = {
                    navController.navigate(Screen.Studio.route)
                },
                onNavigateToOther = {
                    navController.navigate(Screen.Other.route)
                }
            )
        }

        // 履歴画面（日付パラメータ対応）
        composable(
            route = "${Screen.History.route}?date={date}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val epochDay = backStackEntry.arguments?.getLong("date") ?: -1L
            val initialDate = if (epochDay > 0) LocalDate.ofEpochDay(epochDay) else null
            HistoryScreen(
                viewModel = viewModel,
                initialDate = initialDate
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onNavigateToHistory = { date ->
                    navController.navigate(Screen.History.createRouteWithDate(date.toEpochDay()))
                }
            )
        }

        composable(Screen.Graph.route) {
            GraphScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Strength.route) {
            StrengthTrainingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Cardio.route) {
            CardioScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Interval.route) {
            IntervalScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Studio.route) {
            StudioScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Other.route) {
            OtherScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}