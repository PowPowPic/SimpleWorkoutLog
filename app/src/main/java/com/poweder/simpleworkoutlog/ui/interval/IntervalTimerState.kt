package com.poweder.simpleworkoutlog.ui.interval

/**
 * インターバルタイマーのフェーズ状態
 */
enum class IntervalTimerPhase {
    IDLE,           // 待機中
    WARMUP,         // ウォームアップ
    TRAINING,       // トレーニング
    REST,           // インターバル（休憩）
    COOLDOWN,       // クールダウン
    FINISHED        // 完了
}

/**
 * タイマー設定
 */
data class IntervalTimerSettings(
    val warmupSeconds: Int = 10,        // ウォームアップ時間（秒）
    val trainingSeconds: Int = 20,      // トレーニング時間（秒）
    val restSeconds: Int = 10,          // インターバル時間（秒）
    val sets: Int = 8,                  // セット数
    val cooldownSeconds: Int = 30       // クールダウン時間（秒）
) {
    /**
     * 総トレーニング時間（秒）を計算
     */
    fun calculateTotalSeconds(): Int {
        var total = 0
        if (warmupSeconds > 0) total += warmupSeconds
        total += (trainingSeconds + restSeconds) * sets - restSeconds // 最後のrestは不要
        if (cooldownSeconds > 0) total += cooldownSeconds
        return total
    }
    
    companion object {
        /**
         * タバタ式のデフォルト設定
         */
        fun tabataDefault() = IntervalTimerSettings(
            warmupSeconds = 10,
            trainingSeconds = 20,
            restSeconds = 10,
            sets = 8,
            cooldownSeconds = 30
        )
        
        /**
         * HIIT のデフォルト設定
         */
        fun hiitDefault() = IntervalTimerSettings(
            warmupSeconds = 60,
            trainingSeconds = 30,
            restSeconds = 30,
            sets = 10,
            cooldownSeconds = 60
        )
    }
}

/**
 * タイマーの現在状態
 */
data class IntervalTimerState(
    val phase: IntervalTimerPhase = IntervalTimerPhase.IDLE,
    val currentSet: Int = 0,            // 現在のセット（1始まり）
    val totalSets: Int = 0,             // 総セット数
    val remainingSeconds: Int = 0,      // 現在フェーズの残り秒数
    val totalElapsedSeconds: Int = 0,   // 総経過秒数
    val isRunning: Boolean = false
)
