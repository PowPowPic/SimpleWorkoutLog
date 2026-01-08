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
        // テスト用広告ID（本番時は実際のIDに変更）
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * 広告をプリロード
     */
    fun loadAd() {
        if (isLoading || interstitialAd != null) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    interstitialAd = ad
                    isLoading = false
                    setupFullScreenCallback()
                }
            }
        )
    }

    private fun setupFullScreenCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                interstitialAd = null
                loadAd() // 次回用にプリロード
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

    /**
     * 現在の時間帯スロットを取得（0-3）
     */
    private fun getCurrentTimeSlot(): Int {
        val hour = LocalDateTime.now().hour
        return when {
            hour < 6 -> 0   // 0時～6時
            hour < 12 -> 1  // 6時～12時
            hour < 18 -> 2  // 12時～18時
            else -> 3       // 18時～24時
        }
    }

    /**
     * 広告表示可能かチェックして表示
     * @param activity アクティビティ
     * @param onComplete 広告表示完了後（表示しなかった場合も含む）のコールバック
     */
    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val canShow = canShowAdInCurrentSlot()

            if (canShow && interstitialAd != null) {
                // 表示記録を保存
                recordAdShown()
                // 広告表示
                interstitialAd?.show(activity)
                onComplete()
            } else {
                // 広告を表示しない場合
                if (!canShow) {
                    Log.d(TAG, "Ad already shown in this time slot today")
                }
                if (interstitialAd == null) {
                    Log.d(TAG, "Ad not loaded yet")
                    loadAd() // ロードを試みる
                }
                onComplete()
            }
        }
    }

    /**
     * 現在の時間帯で広告表示可能かチェック
     */
    private suspend fun canShowAdInCurrentSlot(): Boolean {
        val today = LocalDate.now().toEpochDay()
        val currentSlot = getCurrentTimeSlot()
        val lastShownDate = settingsDataStore.getInterstitialLastShownDate(currentSlot).first()

        // 今日まだこのスロットで表示していなければtrue
        return lastShownDate != today
    }

    /**
     * 広告表示を記録
     */
    private suspend fun recordAdShown() {
        val today = LocalDate.now().toEpochDay()
        val currentSlot = getCurrentTimeSlot()
        settingsDataStore.setInterstitialLastShownDate(currentSlot, today)
        Log.d(TAG, "Recorded ad shown for slot $currentSlot on day $today")
    }

    /**
     * 広告がロード済みかどうか
     */
    fun isAdLoaded(): Boolean = interstitialAd != null
}