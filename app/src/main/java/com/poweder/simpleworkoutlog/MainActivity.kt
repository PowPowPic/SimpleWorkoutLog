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
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkoutViewModel

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

        setContent {
            SimpleWorkoutLogApp(viewModel = viewModel)
        }
    }
}