package com.poweder.simpleworkoutlog.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.poweder.simpleworkoutlog.data.dao.DailyWorkoutDao
import com.poweder.simpleworkoutlog.data.dao.ExerciseDao
import com.poweder.simpleworkoutlog.data.dao.WorkoutSessionDao
import com.poweder.simpleworkoutlog.data.dao.WorkoutSetDao
import com.poweder.simpleworkoutlog.data.entity.DailyWorkoutEntity
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        DailyWorkoutEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class
    ],
    version = 2,  // 1 → 2 に変更
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun dailyWorkoutDao(): DailyWorkoutDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // マイグレーション: version 1 → 2
        // caloriesBurned カラムを追加
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN caloriesBurned INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_log_database"
                )
                    .addMigrations(MIGRATION_1_2)  // マイグレーション追加
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}