package com.poweder.simpleworkoutlog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.poweder.simpleworkoutlog.billing.BillingManager
import com.poweder.simpleworkoutlog.data.database.AppDatabase
import com.poweder.simpleworkoutlog.data.preferences.LastInputDataStore
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import com.poweder.simpleworkoutlog.data.repository.WorkoutRepository
import com.poweder.simpleworkoutlog.ui.SimpleWorkoutLogApp
import com.poweder.simpleworkoutlog.ui.ads.GoogleMobileAdsConsentManager
import com.poweder.simpleworkoutlog.ui.ads.InterstitialAdManager
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModelFactory
import com.poweder.simpleworkoutlog.util.ReviewHelper

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var interstitialAdManager: InterstitialAdManager
    private lateinit var billingManager: BillingManager
    private lateinit var consentManager: GoogleMobileAdsConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Refresh UMP information on every launch. Review is checked only after the
        // consent flow finishes so the two dialogs cannot overlap.
        consentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
        consentManager.gatherConsent(this) {
            ReviewHelper.checkAndRequest(this)
        }

        // Database & Repository 初期化
        val database = AppDatabase.getInstance(applicationContext)
        val repository = WorkoutRepository(
            exerciseDao = database.exerciseDao(),
            dailyWorkoutDao = database.dailyWorkoutDao(),
            workoutSessionDao = database.workoutSessionDao(),
            workoutSetDao = database.workoutSetDao()
        )
        val settingsDataStore = SettingsDataStore(applicationContext)
        val lastInputDataStore = LastInputDataStore(applicationContext)

        // ViewModel 初期化
        val factory = WorkoutViewModelFactory(
            repository,
            settingsDataStore,
            lastInputDataStore,
            applicationContext
        )
        viewModel = ViewModelProvider(this, factory)[WorkoutViewModel::class.java]

        // 保存済み言語設定を適用
        viewModel.applySavedLanguage()

        // インタースティシャル広告マネージャー初期化
        // 実際の広告ロードはUMP・Mobile Ads初期化・広告削除状態が揃ってから行う。
        interstitialAdManager = InterstitialAdManager(applicationContext, settingsDataStore)

        // 課金マネージャー初期化（接続・購入状態復元）
        billingManager = BillingManager(applicationContext, settingsDataStore)

        setContent {
            SimpleWorkoutLogApp(
                viewModel = viewModel,
                interstitialAdManager = interstitialAdManager,
                billingManager = billingManager
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // UMP側で広告が許可済みの場合のみ、マネージャー内部でプリロードされる。
        if (!interstitialAdManager.isAdLoaded()) {
            interstitialAdManager.loadAd()
        }
        // 購入状態を再チェック（他デバイスでの購入を反映）
        billingManager.restorePurchases()
    }

    override fun onDestroy() {
        interstitialAdManager.setAdsEnabled(false)
        billingManager.destroy()
        super.onDestroy()
    }
}
