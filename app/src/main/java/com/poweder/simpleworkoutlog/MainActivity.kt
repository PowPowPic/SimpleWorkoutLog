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
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModelFactory
import com.poweder.simpleworkoutlog.util.ReviewHelper

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 広告同意フロー撤去後も、アプリ内レビュー判定は独立して継続する。
        ReviewHelper.checkAndRequest(this)

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

        // 課金マネージャー初期化（接続・購入状態復元）
        billingManager = BillingManager(applicationContext, settingsDataStore)

        setContent {
            SimpleWorkoutLogApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // 購入状態を再チェック（他デバイスでの購入を反映）
        billingManager.restorePurchases()
    }

    override fun onDestroy() {
        billingManager.destroy()
        super.onDestroy()
    }
}
