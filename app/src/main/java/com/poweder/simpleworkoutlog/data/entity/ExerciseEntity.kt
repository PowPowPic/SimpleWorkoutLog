package com.poweder.simpleworkoutlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 種目マスタ（例：ベンチプレス、スクワット、ランニングなど）
 * 
 * テンプレート種目（isTemplate=true）:
 *   - templateKey を使用して表示名を取得
 *   - templateKey は strings.xml のキー名と一致（例: "exercise_bench_press"）
 * 
 * カスタム種目（isTemplate=false）:
 *   - customName または name を使用
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val workoutType: String = WorkoutType.STRENGTH,
    val sortOrder: Int = 0,
    val templateKey: String? = null,   // テンプレート種目の安定キー（strings.xmlのキー名）
    val customName: String? = null,    // カスタム名
    val isTemplate: Boolean = false,   // テンプレートかどうか
    val createdAt: Long = System.currentTimeMillis()
)
