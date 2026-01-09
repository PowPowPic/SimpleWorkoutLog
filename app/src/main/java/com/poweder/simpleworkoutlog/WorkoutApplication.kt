package com.poweder.simpleworkoutlog

import android.app.Application
import com.poweder.simpleworkoutlog.data.database.AppDatabase
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorkoutApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初期種目データを登録
        seedInitialExercises()
    }

    private fun seedInitialExercises() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getInstance(applicationContext)
            val exerciseDao = database.exerciseDao()

            // 既存データを取得
            val existingExercises = exerciseDao.getAllExercises().first()

            // 完全に新規の場合
            if (existingExercises.isEmpty()) {
                // 初期筋トレ種目（ベンチプレス、スクワット、デッドリフト）
                val strengthExercises = listOf(
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.STRENGTH,
                        sortOrder = 1,
                        nameResId = R.string.exercise_bench_press,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.STRENGTH,
                        sortOrder = 2,
                        nameResId = R.string.exercise_squat,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.STRENGTH,
                        sortOrder = 3,
                        nameResId = R.string.exercise_deadlift,
                        isTemplate = true
                    )
                )

                // 初期有酸素種目（ラン、ウォーキング、バイク）
                val cardioExercises = listOf(
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.CARDIO,
                        sortOrder = 1,
                        nameResId = R.string.exercise_running,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.CARDIO,
                        sortOrder = 2,
                        nameResId = R.string.exercise_walking,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.CARDIO,
                        sortOrder = 3,
                        nameResId = R.string.exercise_indoor_bike,
                        isTemplate = true
                    )
                )

                // 初期インターバル種目（HIIT、タバタ）
                val intervalExercises = listOf(
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.INTERVAL,
                        sortOrder = 1,
                        nameResId = R.string.exercise_hiit,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.INTERVAL,
                        sortOrder = 2,
                        nameResId = R.string.exercise_tabata,
                        isTemplate = true
                    )
                )

                // 初期スタジオ種目（ヨガ、ピラティス）
                val studioExercises = listOf(
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.STUDIO,
                        sortOrder = 1,
                        nameResId = R.string.exercise_yoga,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.STUDIO,
                        sortOrder = 2,
                        nameResId = R.string.exercise_pilates,
                        isTemplate = true
                    )
                )

                // 初期その他種目（スノーボード、ゴルフ）
                val otherExercises = listOf(
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.OTHER,
                        sortOrder = 1,
                        nameResId = R.string.exercise_snowboarding,
                        isTemplate = true
                    ),
                    ExerciseEntity(
                        name = "",
                        workoutType = WorkoutType.OTHER,
                        sortOrder = 2,
                        nameResId = R.string.exercise_golf,
                        isTemplate = true
                    )
                )

                // データベースに挿入
                (strengthExercises + cardioExercises + intervalExercises + studioExercises + otherExercises).forEach { exercise ->
                    exerciseDao.insert(exercise)
                }
            } else {
                // 既存ユーザー: 「その他」カテゴリに種目がなければ追加
                val otherExercises = existingExercises.filter { it.workoutType == WorkoutType.OTHER }
                if (otherExercises.isEmpty()) {
                    val defaultOtherExercises = listOf(
                        ExerciseEntity(
                            name = "",
                            workoutType = WorkoutType.OTHER,
                            sortOrder = 1,
                            nameResId = R.string.exercise_snowboarding,
                            isTemplate = true
                        ),
                        ExerciseEntity(
                            name = "",
                            workoutType = WorkoutType.OTHER,
                            sortOrder = 2,
                            nameResId = R.string.exercise_golf,
                            isTemplate = true
                        )
                    )
                    defaultOtherExercises.forEach { exercise ->
                        exerciseDao.insert(exercise)
                    }
                }
            }
        }
    }
}