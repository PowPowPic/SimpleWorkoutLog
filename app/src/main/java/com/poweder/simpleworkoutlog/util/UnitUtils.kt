package com.poweder.simpleworkoutlog.util

import java.text.DecimalFormat

/**
 * 重量単位
 */
enum class WeightUnit(val symbol: String, val displayName: String) {
    KG("kg", "Kilograms"),
    LB("lb", "Pounds")
}

/**
 * 距離単位
 */
enum class DistanceUnit(val symbol: String, val displayName: String) {
    KM("km", "Kilometers"),
    MILE("mi", "Miles")
}

// 変換係数
private const val KG_TO_LB = 2.20462
private const val LB_TO_KG = 0.453592
private const val KM_TO_MILE = 0.621371
private const val MILE_TO_KM = 1.60934

/**
 * kg から lb に変換
 */
fun kgToLb(kg: Double): Double = kg * KG_TO_LB

/**
 * lb から kg に変換
 */
fun lbToKg(lb: Double): Double = lb * LB_TO_KG

/**
 * km から mile に変換
 */
fun kmToMile(km: Double): Double = km * KM_TO_MILE

/**
 * mile から km に変換
 */
fun mileToKm(mile: Double): Double = mile * MILE_TO_KM

/**
 * 重量をフォーマット
 */
fun formatWeight(value: Double, unit: WeightUnit): String {
    val df = DecimalFormat("#.#")
    return "${df.format(value)} ${unit.symbol}"
}

/**
 * 距離をフォーマット
 */
fun formatDistance(value: Double, unit: DistanceUnit): String {
    val df = DecimalFormat("#.##")
    return "${df.format(value)} ${unit.symbol}"
}

/**
 * 時間をフォーマット（分:秒）
 */
fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

/**
 * 時間をフォーマット（時:分:秒）
 */
fun formatDurationLong(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
