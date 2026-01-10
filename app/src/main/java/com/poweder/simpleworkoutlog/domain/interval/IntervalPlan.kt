package com.poweder.simpleworkoutlog.domain.interval

/**
 * インターバルタイマーの設定（プラン）
 * 環境依存なし・純データクラス
 */
data class IntervalPlan(
    val warmupSec: Int = 10,         // ウォームアップ時間（秒）
    val trainingSec: Int = 20,       // トレーニング時間（秒）
    val restSec: Int = 10,           // インターバル（休憩）時間（秒）
    val rounds: Int = 8,             // ラウンド数（セット数）
    val cooldownSec: Int = 30        // クールダウン時間（秒）
) {
    /**
     * 総トレーニング時間（秒）を計算
     * warmup + (training + rest) * rounds - rest（最後のrestは不要）+ cooldown
     */
    fun totalDurationSec(): Int {
        var total = 0
        if (warmupSec > 0) total += warmupSec
        // トレーニング + 休憩 × ラウンド数、ただし最後の休憩は不要
        total += trainingSec * rounds + restSec * (rounds - 1).coerceAtLeast(0)
        if (cooldownSec > 0) total += cooldownSec
        return total
    }

    /**
     * 各フェーズの開始時刻（秒）を取得
     * WARMUP: 0
     * TRAINING_1: warmupSec
     * REST_1: warmupSec + trainingSec
     * TRAINING_2: warmupSec + trainingSec + restSec
     * ...
     * COOLDOWN: warmupSec + (trainingSec + restSec) * rounds - restSec
     */
    fun getPhaseStartSec(phase: IntervalPhase, roundIndex: Int = 1): Int {
        return when (phase) {
            IntervalPhase.IDLE -> 0
            IntervalPhase.WARMUP -> 0
            IntervalPhase.TRAINING -> {
                val baseStart = if (warmupSec > 0) warmupSec else 0
                // roundIndex = 1..rounds
                baseStart + (trainingSec + restSec) * (roundIndex - 1)
            }
            IntervalPhase.REST -> {
                val baseStart = if (warmupSec > 0) warmupSec else 0
                // roundIndex = 1..rounds-1 (最後のラウンド後にはRESTがない)
                baseStart + trainingSec + (trainingSec + restSec) * (roundIndex - 1)
            }
            IntervalPhase.COOLDOWN -> {
                val baseStart = if (warmupSec > 0) warmupSec else 0
                baseStart + trainingSec * rounds + restSec * (rounds - 1).coerceAtLeast(0)
            }
            IntervalPhase.FINISHED -> totalDurationSec()
        }
    }

    companion object {
        /**
         * TABATA プリセット
         * 20秒 work / 10秒 rest × 8ラウンド
         * Warm-up / Cooldown なし
         */
        fun tabataDefault() = IntervalPlan(
            warmupSec = 0,
            trainingSec = 20,
            restSec = 10,
            rounds = 8,
            cooldownSec = 0
        )

        /**
         * HIIT プリセット
         * 30秒 work / 30秒 rest × 10ラウンド
         * ユーザーが自由にカスタマイズ可能
         */
        fun hiitDefault() = IntervalPlan(
            warmupSec = 0,
            trainingSec = 30,
            restSec = 30,
            rounds = 10,
            cooldownSec = 0
        )

        /**
         * EMOM プリセット (Every Minute On the Minute)
         * 60秒 work / 0秒 rest × 10ラウンド
         * 毎分スタートを音で知らせる
         */
        fun emomDefault() = IntervalPlan(
            warmupSec = 0,
            trainingSec = 60,
            restSec = 0,
            rounds = 10,
            cooldownSec = 0
        )
    }
}

/**
 * インターバルタイマーのフェーズ
 */
enum class IntervalPhase {
    IDLE,       // 待機中（開始前）
    WARMUP,     // ウォームアップ
    TRAINING,   // トレーニング（ワーク）
    REST,       // インターバル（休憩）
    COOLDOWN,   // クールダウン
    FINISHED    // 完了
}
