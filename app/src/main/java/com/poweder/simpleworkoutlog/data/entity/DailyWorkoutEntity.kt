package com.poweder.simpleworkoutlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日別ワークアウト集計データ
 */
@Entity(tableName = "daily_workouts")
data class DailyWorkoutEntity(
    @PrimaryKey
    val date: Long,                          // 日付（EpochDay）
    val totalWeight: Double = 0.0,           // 総挙上重量
    val totalSets: Int = 0,                  // 総セット数
    val totalReps: Int = 0,                  // 総レップ数
    val totalDurationSeconds: Int = 0,       // 総運動時間（秒）
    val totalCalories: Int = 0,              // 総消費カロリー（kcal）
    val updatedAt: Long = System.currentTimeMillis()
)