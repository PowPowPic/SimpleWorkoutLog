package com.poweder.simpleworkoutlog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.MobileAds
import com.poweder.simpleworkoutlog.data.database.AppDatabase
import com.poweder.simpleworkoutlog.data.preferences.LastInputDataStore
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import com.poweder.simpleworkoutlog.data.repository.WorkoutRepository
import com.poweder.simpleworkoutlog.ui.SimpleWorkoutLogApp
import com.poweder.simpleworkoutlog.ui.ads.InterstitialAdManager
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mobile Ads 初期化
        MobileAds.initialize(this) {}

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
        interstitialAdManager = InterstitialAdManager(applicationContext, settingsDataStore)
        interstitialAdManager.loadAd() // プリロード開始

        setContent {
            SimpleWorkoutLogApp(
                viewModel = viewModel,
                interstitialAdManager = interstitialAdManager
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // アプリ復帰時に広告をプリロード（まだロードされていない場合）
        if (!interstitialAdManager.isAdLoaded()) {
            interstitialAdManager.loadAd()
        }
    }
}