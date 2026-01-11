package com.poweder.simpleworkoutlog

import android.app.Application
import com.poweder.simpleworkoutlog.data.database.AppDatabase
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // テンプレート種目の初期化・更新
        ensureLatestTemplates()
    }

    /**
     * テンプレート種目を最新状態に保つ
     * - 旧テンプレ（isTemplate=true）をすべて削除
     * - 最新テンプレ（templateKey方式）を挿入
     * - カスタム種目（isTemplate=false）は一切触らない
     * 
     * これにより、アップデート後のnameResId化け問題を根治する
     */
    private fun ensureLatestTemplates() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getInstance(applicationContext)
            val exerciseDao = database.exerciseDao()

            // 旧テンプレをすべて削除
            exerciseDao.deleteTemplates()

            // 最新テンプレを挿入
            insertLatestTemplates(exerciseDao)
        }
    }

    /**
     * 最新テンプレート種目を挿入
     * templateKey は strings.xml のキー名と一致させる
     */
    private suspend fun insertLatestTemplates(exerciseDao: com.poweder.simpleworkoutlog.data.dao.ExerciseDao) {
        // 筋トレ種目
        val strengthExercises = listOf(
            ExerciseEntity(
                workoutType = WorkoutType.STRENGTH,
                sortOrder = 1,
                templateKey = "exercise_bench_press",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.STRENGTH,
                sortOrder = 2,
                templateKey = "exercise_squat",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.STRENGTH,
                sortOrder = 3,
                templateKey = "exercise_deadlift",
                isTemplate = true
            )
        )

        // 有酸素運動種目
        val cardioExercises = listOf(
            ExerciseEntity(
                workoutType = WorkoutType.CARDIO,
                sortOrder = 1,
                templateKey = "exercise_running",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.CARDIO,
                sortOrder = 2,
                templateKey = "exercise_walking",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.CARDIO,
                sortOrder = 3,
                templateKey = "exercise_indoor_bike",
                isTemplate = true
            )
        )

        // インターバル種目（順序: TABATA → HIIT → EMOM）
        val intervalExercises = listOf(
            ExerciseEntity(
                workoutType = WorkoutType.INTERVAL,
                sortOrder = 1,
                templateKey = "exercise_tabata",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.INTERVAL,
                sortOrder = 2,
                templateKey = "exercise_hiit",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.INTERVAL,
                sortOrder = 3,
                templateKey = "exercise_emom",
                isTemplate = true
            )
        )

        // スタジオ種目
        val studioExercises = listOf(
            ExerciseEntity(
                workoutType = WorkoutType.STUDIO,
                sortOrder = 1,
                templateKey = "exercise_yoga",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.STUDIO,
                sortOrder = 2,
                templateKey = "exercise_pilates",
                isTemplate = true
            )
        )

        // その他種目
        val otherExercises = listOf(
            ExerciseEntity(
                workoutType = WorkoutType.OTHER,
                sortOrder = 1,
                templateKey = "exercise_snowboarding",
                isTemplate = true
            ),
            ExerciseEntity(
                workoutType = WorkoutType.OTHER,
                sortOrder = 2,
                templateKey = "exercise_golf",
                isTemplate = true
            )
        )

        // データベースに挿入
        (strengthExercises + cardioExercises + intervalExercises + studioExercises + otherExercises).forEach { exercise ->
            exerciseDao.insert(exercise)
        }
    }
}