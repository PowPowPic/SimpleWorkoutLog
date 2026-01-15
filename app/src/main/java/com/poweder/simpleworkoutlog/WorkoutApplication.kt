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

        // テンプレート種目の初期化・更新（Upsert方式）
        ensureLatestTemplates()
    }

    /**
     * テンプレート種目を最新状態に保つ（Upsert方式）
     * 
     * ⚠️ 重要：テンプレートは「削除」してはいけない
     * WorkoutSessionEntity が ExerciseEntity を ForeignKey.CASCADE で参照しているため、
     * テンプレートを削除すると、それに紐づくセッション・セットが連鎖削除される。
     * 
     * 方式：
     * - templateKey で既存テンプレを検索
     * - あれば sortOrder のみ更新（idは維持 → 外部キー参照を壊さない）
     * - なければ新規挿入
     * - カスタム種目（isTemplate=false）は一切触らない
     */
    private fun ensureLatestTemplates() {
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.getInstance(applicationContext)
            val exerciseDao = database.exerciseDao()

            // 最新テンプレをUpsert（削除は行わない）
            upsertLatestTemplates(exerciseDao)
        }
    }

    /**
     * 最新テンプレート種目をUpsert
     * templateKey は strings.xml のキー名と一致させる
     */
    private suspend fun upsertLatestTemplates(exerciseDao: com.poweder.simpleworkoutlog.data.dao.ExerciseDao) {
        // 全テンプレート定義
        val allTemplates = listOf(
            // 筋トレ種目
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
            ),

            // 有酸素運動種目
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
            ),

            // インターバル種目（順序: TABATA → HIIT → EMOM）
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
            ),

            // スタジオ種目
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
            ),

            // その他種目
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

        // 各テンプレートをUpsert（既存ならsortOrder更新、なければ挿入）
        allTemplates.forEach { template ->
            exerciseDao.upsertTemplate(template)
        }
    }
}
