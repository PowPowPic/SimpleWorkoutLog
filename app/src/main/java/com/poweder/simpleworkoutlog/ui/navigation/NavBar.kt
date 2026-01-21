package com.poweder.simpleworkoutlog.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * HorizontalPager連携版BottomNavBar
 * 
 * 高齢者対応：
 * - フォントサイズは見た目固定（12f）でOSの文字サイズ設定の影響を受けない
 * - 改行禁止、収まらない場合は省略表示（...）
 *
 * @param currentPage 現在のPagerページ（0-4）
 * @param onPageSelected タブクリック時のコールバック（ページ番号を渡す）
 */
@Composable
fun BottomNavBar(
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    // OSの文字サイズ設定を打ち消して見た目固定12fにする
    val config = LocalConfiguration.current
    val fixedLabelSize = (12f / config.fontScale).sp

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
                label = {
                    Text(
                        text = stringResource(screen.resourceId),
                        fontSize = fixedLabelSize,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
