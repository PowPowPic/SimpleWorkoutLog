package com.poweder.simpleworkoutlog.data.model

import java.util.UUID

/**
 * ワークアウトタイプのデータモデル
 * - デフォルト3タイプ + ユーザー追加タイプ
 */
data class WorkoutTypeItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameResId: Int? = null,  // リソースID（デフォルトタイプ用）
    val isDefault: Boolean = false,  // デフォルトタイプかどうか
    val sortOrder: Int = 0
) {
    companion object {
        const val TYPE_STRENGTH = "STRENGTH"
        const val TYPE_CARDIO = "CARDIO"
        const val TYPE_INTERVAL = "INTERVAL"
    }
}
