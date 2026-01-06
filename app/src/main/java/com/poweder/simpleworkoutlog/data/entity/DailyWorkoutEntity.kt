package com.poweder.simpleworkoutlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日別ワークアウトサマリー
 * Grand Totalの集計用
 */
@Entity(tableName = "daily_workouts")
data class DailyWorkoutEntity(
    @PrimaryKey
    val logicalDate: Long,              // EpochDay
    val totalWeight: Double = 0.0,      // 筋トレ総重量（kg）
    val totalCardioMinutes: Int = 0,    // 有酸素運動時間（分）
    val totalIntervalMinutes: Int = 0,  // インターバル時間（分）
    val updatedAt: Long = System.currentTimeMillis()
)
