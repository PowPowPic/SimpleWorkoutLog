package com.poweder.simpleworkoutlog.data.model

import java.util.UUID

/**
 * セット入力行のデータモデル
 * @param id ユニークID（UI識別用）
 * @param setNumber セット番号（1, 2, 3...）
 * @param weight 重量
 * @param reps レップ数
 * @param isConfirmed 確定済みかどうか
 */
data class SetItem(
    val id: String = UUID.randomUUID().toString(),
    val setNumber: Int,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val isConfirmed: Boolean = false
) {
    /**
     * トータル重量（weight × reps）
     */
    val totalWeight: Double
        get() = weight * reps
    
    /**
     * 入力が有効かどうか（weight > 0 かつ reps > 0）
     */
    val isValid: Boolean
        get() = weight > 0 && reps > 0
}
