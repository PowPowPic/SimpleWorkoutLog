package com.poweder.simpleworkoutlog.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poweder.simpleworkoutlog.ui.ads.InterstitialAdManager
import com.poweder.simpleworkoutlog.ui.cardio.CardioScreen
import com.poweder.simpleworkoutlog.ui.interval.IntervalScreen
import com.poweder.simpleworkoutlog.ui.navigation.BottomNavBar
import com.poweder.simpleworkoutlog.ui.navigation.Screen
import com.poweder.simpleworkoutlog.ui.other.OtherScreen
import com.poweder.simpleworkoutlog.ui.screen.CalendarScreen
import com.poweder.simpleworkoutlog.ui.screen.GraphScreen
import com.poweder.simpleworkoutlog.ui.screen.HistoryScreen
import com.poweder.simpleworkoutlog.ui.screen.MainScreen
import com.poweder.simpleworkoutlog.ui.settings.SettingsScreen
import com.poweder.simpleworkoutlog.ui.strength.StrengthTrainingScreen
import com.poweder.simpleworkoutlog.ui.studio.StudioScreen
import com.poweder.simpleworkoutlog.ui.theme.SimpleWorkoutLogTheme
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

/**
 * メインApp - HorizontalPagerによるスワイプ遷移対応版
 *
 * 構造:
 * - NavHost: "main_pager"（メイン5画面）と各種目画面（Strength, Cardio等）を管理
 * - HorizontalPager: メイン5画面間のスワイプ遷移
 * - BottomNavBar: タブクリックでPagerを操作（グラフタブのみ広告判定）
 *
 * 編集モード:
 * - sessionId = -1 → 新規作成モード
 * - sessionId > 0 → 編集モード（既存セッションをロード）
 */
@Composable
fun SimpleWorkoutLogApp(
    viewModel: WorkoutViewModel,
    interstitialAdManager: InterstitialAdManager? = null
) {
    SimpleWorkoutLogTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val context = LocalContext.current
        val activity = context as? Activity

        // メイン5画面のPager状態（初期ページ: 0 = Home）
        val pagerState = rememberPagerState(pageCount = { 5 })
        val coroutineScope = rememberCoroutineScope()

        // メイン画面かどうか（PagerとNavBarを表示するか）
        val isMainScreen = currentRoute == null || currentRoute == "main_pager"

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
                containerColor = Color.Transparent,
                bottomBar = {
                    if (isMainScreen) {
                        BottomNavBar(
                            currentPage = pagerState.currentPage,
                            onPageSelected = { index ->
                                // グラフタブ（index=3）のボタンクリック時のみ広告判定
                                if (index == 3 && activity != null && interstitialAdManager != null) {
                                    interstitialAdManager.showAdIfAvailable(activity) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "main_pager",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    // メイン5画面（HorizontalPager）
                    composable("main_pager") {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1 // 隣接ページをプリロード
                        ) { page ->
                            when (page) {
                                0 -> MainScreen(
                                    viewModel = viewModel,
                                    onSettingsClick = {
                                        // Settingsタブへ移動
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(4)
                                        }
                                    },
                                    onNavigateToStrength = {
                                        // 新規作成（sessionId = -1）
                                        navController.navigate(Screen.Strength.createRoute(null))
                                    },
                                    onNavigateToCardio = {
                                        navController.navigate(Screen.Cardio.createRoute(null))
                                    },
                                    onNavigateToInterval = {
                                        navController.navigate(Screen.Interval.createRoute(null))
                                    },
                                    onNavigateToStudio = {
                                        navController.navigate(Screen.Studio.createRoute(null))
                                    },
                                    onNavigateToOther = {
                                        navController.navigate(Screen.Other.createRoute(null))
                                    }
                                )
                                1 -> HistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateToStrengthEdit = { sessionId ->
                                        navController.navigate(Screen.Strength.createRoute(sessionId))
                                    },
                                    onNavigateToCardioEdit = { sessionId ->
                                        navController.navigate(Screen.Cardio.createRoute(sessionId))
                                    },
                                    onNavigateToIntervalEdit = { sessionId ->
                                        navController.navigate(Screen.Interval.createRoute(sessionId))
                                    },
                                    onNavigateToStudioEdit = { sessionId ->
                                        navController.navigate(Screen.Studio.createRoute(sessionId))
                                    },
                                    onNavigateToOtherEdit = { sessionId ->
                                        navController.navigate(Screen.Other.createRoute(sessionId))
                                    }
                                )
                                2 -> CalendarScreen(
                                    viewModel = viewModel,
                                    onNavigateToStrengthEdit = { sessionId ->
                                        navController.navigate(Screen.Strength.createRoute(sessionId))
                                    },
                                    onNavigateToCardioEdit = { sessionId ->
                                        navController.navigate(Screen.Cardio.createRoute(sessionId))
                                    },
                                    onNavigateToIntervalEdit = { sessionId ->
                                        navController.navigate(Screen.Interval.createRoute(sessionId))
                                    },
                                    onNavigateToStudioEdit = { sessionId ->
                                        navController.navigate(Screen.Studio.createRoute(sessionId))
                                    },
                                    onNavigateToOtherEdit = { sessionId ->
                                        navController.navigate(Screen.Other.createRoute(sessionId))
                                    }
                                )
                                3 -> GraphScreen(viewModel = viewModel)
                                4 -> SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        // Homeタブへ戻る
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 種目別画面（NavBarなし）- sessionId対応
                    composable(
                        route = Screen.Strength.route,
                        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                        StrengthTrainingScreen(
                            viewModel = viewModel,
                            sessionId = if (sessionId == -1L) null else sessionId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Cardio.route,
                        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                        CardioScreen(
                            viewModel = viewModel,
                            sessionId = if (sessionId == -1L) null else sessionId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Interval.route,
                        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                        IntervalScreen(
                            viewModel = viewModel,
                            sessionId = if (sessionId == -1L) null else sessionId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Studio.route,
                        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                        StudioScreen(
                            viewModel = viewModel,
                            sessionId = if (sessionId == -1L) null else sessionId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Other.route,
                        arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                        OtherScreen(
                            viewModel = viewModel,
                            sessionId = if (sessionId == -1L) null else sessionId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}