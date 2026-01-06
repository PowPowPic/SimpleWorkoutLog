package com.poweder.simpleworkoutlog.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ワークアウトセッション（1回のトレーニング）
 * 例：1月6日のベンチプレスセッション
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId"), Index("logicalDate")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val logicalDate: Long,               // EpochDay
    val workoutType: String,             // STRENGTH, CARDIO, INTERVAL
    val totalWeight: Double = 0.0,       // 筋トレ用：総重量
    val durationMinutes: Int = 0,        // 有酸素/インターバル用：時間
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
