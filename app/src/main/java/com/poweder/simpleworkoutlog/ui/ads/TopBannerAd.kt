package com.poweder.simpleworkoutlog.ui.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun TopBannerAd(
    showAd: Boolean,
    modifier: Modifier = Modifier
) {
    if (showAd) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.Transparent), // 背景を透明に
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = "ca-app-pub-3940256099942544/6300978111" // テスト広告ID
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    // showAd = false の場合は何も表示しない（高さ0）
}