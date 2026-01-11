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
    version = 6,  // 5 → 6: nameResId撤廃、templateKey追加
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

        /**
         * Migration 5→6: nameResId を撤廃し、templateKey を追加
         * SQLiteはカラム削除をサポートしないため、テーブルを再作成する
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 新しいテーブルを作成（nameResIdなし、templateKeyあり）
                db.execSQL("""
                    CREATE TABLE exercises_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        workoutType TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        templateKey TEXT,
                        customName TEXT,
                        isTemplate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. 旧テーブルから新テーブルへデータをコピー（nameResIdは除外）
                db.execSQL("""
                    INSERT INTO exercises_new (id, name, workoutType, sortOrder, templateKey, customName, isTemplate, createdAt)
                    SELECT id, name, workoutType, sortOrder, NULL, customName, isTemplate, createdAt
                    FROM exercises
                """.trimIndent())

                // 3. 旧テーブルを削除
                db.execSQL("DROP TABLE exercises")

                // 4. 新テーブルを正式名にリネーム
                db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_log_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    // クローズドテスト中：Migrationが失敗した場合のfallback
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}