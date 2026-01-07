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
    version = 3,  // 2 → 3 に変更
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

        // マイグレーション: version 2 → 3
// daily_workouts テーブルを再作成（logicalDate → date、totalDuration、totalCalories追加）
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 新しいテーブルを作成
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_workouts_new (
                date INTEGER NOT NULL PRIMARY KEY,
                totalWeight REAL NOT NULL DEFAULT 0.0,
                totalSets INTEGER NOT NULL DEFAULT 0,
                totalReps INTEGER NOT NULL DEFAULT 0,
                totalDuration INTEGER NOT NULL DEFAULT 0,
                totalCalories INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """)

                // 2. 古いデータを新しいテーブルにコピー（存在するカラムのみ）
                database.execSQL("""
            INSERT INTO daily_workouts_new (date, totalWeight, totalSets, totalReps, totalDuration, totalCalories, updatedAt)
            SELECT logicalDate, totalWeight, 0, 0, 0, 0, updatedAt
            FROM daily_workouts
        """)

                // 3. 古いテーブルを削除
                database.execSQL("DROP TABLE daily_workouts")

                // 4. 新しいテーブルをリネーム
                database.execSQL("ALTER TABLE daily_workouts_new RENAME TO daily_workouts")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_log_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // マイグレーション追加
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}