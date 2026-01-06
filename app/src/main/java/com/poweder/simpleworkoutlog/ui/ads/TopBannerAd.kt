package com.poweder.simpleworkoutlog.ui.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * 上部バナー広告（または広告削除時の余白）
 * @param showAd 広告を表示するかどうか（課金削除時はfalse）
 */
@Composable
fun TopBannerAd(
    showAd: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 広告の高さを固定（広告削除時も余白として維持）
    val adHeight = 50.dp
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeight)
            .background(WorkoutColors.BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        if (showAd) {
            val context = LocalContext.current
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        // テスト広告ID
                        adUnitId = "ca-app-pub-3940256099942544/6300978111"
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        // showAd = false の場合は空のBoxが余白として残る
    }
}
