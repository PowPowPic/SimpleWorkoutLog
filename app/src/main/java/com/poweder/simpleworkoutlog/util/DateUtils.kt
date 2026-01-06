package com.poweder.simpleworkoutlog.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 深夜ルール：0:00〜3:00は前日扱い
 * 例：1月6日 2:30 → 1月5日として記録
 */
fun currentLogicalDate(): LocalDate {
    val now = LocalDateTime.now()
    val cutoffTime = LocalTime.of(3, 0) // 午前3時
    
    return if (now.toLocalTime().isBefore(cutoffTime)) {
        now.toLocalDate().minusDays(1)
    } else {
        now.toLocalDate()
    }
}

/**
 * 指定された日時から論理日付を取得
 */
fun getLogicalDate(dateTime: LocalDateTime): LocalDate {
    val cutoffTime = LocalTime.of(3, 0)
    
    return if (dateTime.toLocalTime().isBefore(cutoffTime)) {
        dateTime.toLocalDate().minusDays(1)
    } else {
        dateTime.toLocalDate()
    }
}

/**
 * 週の開始日（月曜日）を取得
 */
fun getWeekStartDate(date: LocalDate = currentLogicalDate()): LocalDate {
    val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday
    return date.minusDays((dayOfWeek - 1).toLong())
}

/**
 * 週の終了日（日曜日）を取得
 */
fun getWeekEndDate(date: LocalDate = currentLogicalDate()): LocalDate {
    val dayOfWeek = date.dayOfWeek.value
    return date.plusDays((7 - dayOfWeek).toLong())
}

/**
 * 月の開始日を取得
 */
fun getMonthStartDate(date: LocalDate = currentLogicalDate()): LocalDate {
    return date.withDayOfMonth(1)
}

/**
 * 月の終了日を取得
 */
fun getMonthEndDate(date: LocalDate = currentLogicalDate()): LocalDate {
    return date.withDayOfMonth(date.lengthOfMonth())
}
