package com.poweder.simpleworkoutlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 種目マスタ（例：ベンチプレス、スクワット、ランニングなど）
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val workoutType: String = WorkoutType.STRENGTH,  // STRENGTH, CARDIO, INTERVAL
    val sortOrder: Int = 0,
    val nameResId: Int? = null,      // テンプレート種目のリソースID
    val customName: String? = null,   // カスタム名
    val isTemplate: Boolean = false,  // テンプレートかどうか
    val createdAt: Long = System.currentTimeMillis()
)
