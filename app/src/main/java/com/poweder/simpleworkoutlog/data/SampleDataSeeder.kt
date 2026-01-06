package com.poweder.simpleworkoutlog.data

import android.content.Context
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.dao.ExerciseDao
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType

class SampleDataSeeder(
    private val context: Context,
    private val exerciseDao: ExerciseDao
) {
    suspend fun seedIfEmpty() {
        val exercises = exerciseDao.getAllExercises()
        // Flowなので最初の値を取得する必要がある
        // 初期化時は空の場合のみシード
    }
    
    suspend fun seedSampleData() {
        // 筋トレ種目
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
            ),
            ExerciseEntity(
                name = "",
                workoutType = WorkoutType.STRENGTH,
                sortOrder = 4,
                nameResId = R.string.exercise_overhead_press,
                isTemplate = true
            ),
            ExerciseEntity(
                name = "",
                workoutType = WorkoutType.STRENGTH,
                sortOrder = 5,
                nameResId = R.string.exercise_barbell_row,
                isTemplate = true
            )
        )
        
        // 有酸素運動種目
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
                nameResId = R.string.exercise_cycling,
                isTemplate = true
            ),
            ExerciseEntity(
                name = "",
                workoutType = WorkoutType.CARDIO,
                sortOrder = 3,
                nameResId = R.string.exercise_swimming,
                isTemplate = true
            )
        )
        
        // インターバル種目
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
        
        (strengthExercises + cardioExercises + intervalExercises).forEach {
            exerciseDao.insert(it)
        }
    }
}
