package com.poweder.simpleworkoutlog.util

/**
 * 運動時間フォーマット共通ユーティリティ
 *
 * 保存: 秒(Int)
 * 表示: h:mm:ss
 * 入力: 30 / 30:30 / 1:15:00 を許容
 */

/**
 * 秒数を h:mm:ss 形式にフォーマット
 * 例: 3661 → "1:01:01"
 *     270 → "0:04:30"
 *     0 → "0:00:00"
 */
fun formatHms(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%d:%02d:%02d".format(hours, minutes, seconds)
}

/**
 * h:mm:ss / mm:ss / 分のみ の入力を秒に変換
 *
 * パース規則:
 * - "30" → 30分 = 1800秒（互換性のため数字のみは分として扱う）
 * - "30:30" → 30分30秒 = 1830秒
 * - "1:15:00" → 1時間15分00秒 = 4500秒
 * - 不正値 → 0秒
 */
fun parseHmsToSeconds(input: String): Int {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return 0

    // コロンで分割
    val parts = trimmed.split(":")

    return try {
        when (parts.size) {
            1 -> {
                // 数字のみ → 分として扱う（互換性のため）
                val minutes = parts[0].toIntOrNull() ?: 0
                minutes * 60
            }
            2 -> {
                // mm:ss 形式
                val minutes = parts[0].toIntOrNull() ?: 0
                val seconds = parts[1].toIntOrNull() ?: 0
                minutes * 60 + seconds
            }
            3 -> {
                // h:mm:ss 形式
                val hours = parts[0].toIntOrNull() ?: 0
                val minutes = parts[1].toIntOrNull() ?: 0
                val seconds = parts[2].toIntOrNull() ?: 0
                hours * 3600 + minutes * 60 + seconds
            }
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
}

/**
 * 秒数を短い形式にフォーマット（履歴表示用）
 * 例: 3661 → "1h 1m"
 *     270 → "4m 30s"
 *     0 → "0m"
 */
fun formatDurationShort(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0m"
    }
}