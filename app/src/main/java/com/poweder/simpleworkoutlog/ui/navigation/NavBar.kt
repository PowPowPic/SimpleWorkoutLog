package com.poweder.simpleworkoutlog.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * HorizontalPager連携版BottomNavBar
 *
 * @param currentPage 現在のPagerページ（0-4）
 * @param onPageSelected タブクリック時のコールバック（ページ番号を渡す）
 */
@Composable
fun BottomNavBar(
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = WorkoutColors.NavBarBackground
    ) {
        Screen.bottomNavItems.forEachIndexed { index, screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.resourceId)
                    )
                },
                label = { Text(stringResource(screen.resourceId)) },
                selected = currentPage == index,
                onClick = {
                    if (currentPage != index) {
                        onPageSelected(index)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WorkoutColors.NavBarIconSelected,
                    selectedTextColor = WorkoutColors.NavBarIconSelected,
                    unselectedIconColor = WorkoutColors.NavBarIconUnselected,
                    unselectedTextColor = WorkoutColors.NavBarIconUnselected,
                    indicatorColor = WorkoutColors.NavBarBackground
                )
            )
        }
    }
}