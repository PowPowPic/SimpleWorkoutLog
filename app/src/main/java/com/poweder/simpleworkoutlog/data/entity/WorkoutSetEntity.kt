package com.poweder.simpleworkoutlog.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * セット詳細（筋トレ用）
 * 例：ベンチプレス 50kg × 8 reps = 400kg
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val setNumber: Int,                  // セット番号（1, 2, 3...）
    val weight: Double,                  // 重量（kg）
    val reps: Int,                       // レップ数
    val totalWeight: Double,             // weight × reps
    val createdAt: Long = System.currentTimeMillis()
)
