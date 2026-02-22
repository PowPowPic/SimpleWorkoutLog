package com.poweder.simpleworkoutlog.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

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
 * ロケールの小数点記号を取得
 * ドイツ語・フランス語などは ',' 、英語・日本語などは '.'
 */
fun getDecimalSeparator(locale: Locale = Locale.getDefault()): Char {
    return DecimalFormatSymbols(locale).decimalSeparator
}

/**
 * ロケール対応の小数を含む正規表現パターンを返す
 * 入力フィールドのバリデーションに使用
 * 例: ロケールが ',' 小数点なら "^\d*(,)?\d*$"
 *     ロケールが '.' 小数点なら "^\d*(\.)?\ d*$"
 */
fun getDecimalInputRegex(locale: Locale = Locale.getDefault()): Regex {
    val sep = getDecimalSeparator(locale)
    return if (sep == ',') {
        Regex("^\\d*(,)?\\d*$")
    } else {
        Regex("^\\d*(\\.)?\\d*$")
    }
}

/**
 * ロケール対応の文字列を Double にパース
 * "1,9" (ドイツ語) も "1.9" (英語) もどちらも 1.9 として解釈する
 */
fun parseLocalizedDouble(value: String): Double? {
    if (value.isBlank()) return null
    // カンマを小数点ピリオドに置換して統一パース
    return value.replace(',', '.').toDoubleOrNull()
}

/**
 * Double を入力フィールド表示用にロケール対応フォーマット
 * 例: ドイツ語なら 1.9 → "1,9"、英語なら 1.9 → "1.9"
 * 桁区切りは入力フィールドには含めない（入力しやすさのため）
 */
fun formatDoubleForInput(value: Double, decimals: Int = 2, locale: Locale = Locale.getDefault()): String {
    if (value <= 0) return ""
    val df = DecimalFormat().apply {
        decimalFormatSymbols = DecimalFormatSymbols(locale)
        isGroupingUsed = false  // 入力フィールドには桁区切りを入れない
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
    }
    return df.format(value)
}

/**
 * 重量をフォーマット（桁区切り・小数点ロケール対応）
 * 例: ドイツ語で 12360.5 kg → "12.360,5 kg"
 *     英語で 12360.5 lb → "12,360.5 lb"
 */
fun formatWeight(value: Double, unit: WeightUnit): String {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }
    return "${nf.format(value)} ${unit.symbol}"
}

/**
 * 距離をフォーマット（桁区切り・小数点ロケール対応）
 * 例: ドイツ語で 1.9 km → "1,9 km"
 *     英語で 10000.5 km → "10,000.5 km"
 */
fun formatDistance(value: Double, unit: DistanceUnit): String {
    val nf = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    return "${nf.format(value)} ${unit.symbol}"
}

/**
 * カロリーをフォーマット（桁区切りロケール対応）
 * 例: ドイツ語で 1000 → "1.000 kcal"
 *     英語で 1000 → "1,000 kcal"
 */
fun formatCalories(value: Int): String {
    val nf = NumberFormat.getIntegerInstance(Locale.getDefault())
    return "${nf.format(value)} kcal"
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