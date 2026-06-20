package com.poweder.simpleworkoutlog.ui.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * インタースティシャル広告マネージャー
 *
 * 時間帯制御:
 * - 0時～6時: スロット0
 * - 6時～12時: スロット1
 * - 12時～18時: スロット2
 * - 18時～24時: スロット3
 *
 * 各スロットで1日1回まで表示可能
 */
class InterstitialAdManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val TAG = "InterstitialAdManager"
        private const val AD_UNIT_ID = "ca-app-pub-7305983073191908/5236598231"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    @Volatile
    private var adsEnabled = false

    /** Apply the shared UMP, SDK-initialization, and ad-removal gate. */
    fun setAdsEnabled(enabled: Boolean) {
        adsEnabled = enabled
        if (enabled) {
            loadAd()
        } else {
            interstitialAd = null
            isLoading = false
        }
    }

    /** 広告をプリロード */
    fun loadAd() {
        if (!adsEnabled || isLoading || interstitialAd != null) return

        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    isLoading = false
                    if (adsEnabled) {
                        interstitialAd = ad
                        setupFullScreenCallback()
                    } else {
                        interstitialAd = null
                    }
                }
            }
        )
    }

    private fun setupFullScreenCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                interstitialAd = null
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.d(TAG, "Ad failed to show: ${error.message}")
                interstitialAd = null
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed")
            }
        }
    }

    private fun getCurrentTimeSlot(): Int {
        val hour = LocalDateTime.now().hour
        return when {
            hour < 6 -> 0
            hour < 12 -> 1
            hour < 18 -> 2
            else -> 3
        }
    }

    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit) {
        if (!adsEnabled) {
            onComplete()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val adRemoved = settingsDataStore.adRemovedFlow.first()
            if (adRemoved || !adsEnabled) {
                onComplete()
                return@launch
            }

            val canShow = canShowAdInCurrentSlot()

            if (canShow && interstitialAd != null && adsEnabled) {
                recordAdShown()
                interstitialAd?.show(activity)
                onComplete()
            } else {
                if (!canShow) {
                    Log.d(TAG, "Ad already shown in this time slot today")
                }
                if (interstitialAd == null) {
                    Log.d(TAG, "Ad not loaded yet")
                    loadAd()
                }
                onComplete()
            }
        }
    }

    private suspend fun canShowAdInCurrentSlot(): Boolean {
        val today = LocalDate.now().toEpochDay()
        val currentSlot = getCurrentTimeSlot()
        val lastShownDate = settingsDataStore.getInterstitialLastShownDate(currentSlot).first()
        return lastShownDate != today
    }

    private suspend fun recordAdShown() {
        val today = LocalDate.now().toEpochDay()
        val currentSlot = getCurrentTimeSlot()
        settingsDataStore.setInterstitialLastShownDate(currentSlot, today)
        Log.d(TAG, "Recorded ad shown for slot $currentSlot on day $today")
    }

    fun isAdLoaded(): Boolean = interstitialAd != null
}
