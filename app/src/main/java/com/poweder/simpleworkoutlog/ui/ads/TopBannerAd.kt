package com.poweder.simpleworkoutlog.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val consentManager = remember(context.applicationContext) {
        GoogleMobileAdsConsentManager.getInstance(context.applicationContext)
    }
    val consentGatheringComplete by consentManager.consentGatheringComplete.collectAsState()
    val canRequestAds by consentManager.canRequestAds.collectAsState()
    val mobileAdsInitialized by consentManager.mobileAdsInitialized.collectAsState()

    val shouldLoadAd =
        showAd && consentGatheringComplete && canRequestAds && mobileAdsInitialized

    // 広告の可否にかかわらず従来どおり50dpの領域を維持する。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        if (shouldLoadAd) {
            val adView = remember(context) {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-7305983073191908/2304087212"
                }
            }

            DisposableEffect(adView) {
                adView.loadAd(AdRequest.Builder().build())
                onDispose {
                    adView.destroy()
                }
            }

            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
