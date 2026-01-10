package com.poweder.simpleworkoutlog.domain.interval

/**
 * インターバルタイマーの現在状態スナップショット
 * TimerEngineが計算した結果をUIに渡すための純データクラス
 * 
 * 環境依存なし・イミュータブル
 */
data class IntervalSnapshot(
    val phase: IntervalPhase,        // 現在のフェーズ
    val roundIndex: Int,             // 現在のラウンド（1..rounds、FINISHEDならrounds）
    val totalRounds: Int,            // 総ラウンド数
    val phaseRemainingSec: Int,      // 現在フェーズの残り秒数（0以上）
    val phaseTotalSec: Int,          // 現在フェーズの総秒数
    val totalElapsedSec: Int,        // 開始からの総経過秒数
    val totalDurationSec: Int,       // 総トレーニング時間
    val isRunning: Boolean           // タイマー動作中かどうか
) {
    /**
     * 現在フェーズの経過秒数
     */
    val phaseElapsedSec: Int
        get() = (phaseTotalSec - phaseRemainingSec).coerceAtLeast(0)

    /**
     * 現在フェーズの進捗率（0.0〜1.0）
     */
    val phaseProgress: Float
        get() = if (phaseTotalSec > 0) {
            phaseElapsedSec.toFloat() / phaseTotalSec
        } else {
            0f
        }

    /**
     * 全体の進捗率（0.0〜1.0）
     */
    val totalProgress: Float
        get() = if (totalDurationSec > 0) {
            totalElapsedSec.toFloat() / totalDurationSec
        } else {
            0f
        }

    companion object {
        /**
         * 初期状態（IDLE）
         */
        fun idle(plan: IntervalPlan) = IntervalSnapshot(
            phase = IntervalPhase.IDLE,
            roundIndex = 0,
            totalRounds = plan.rounds,
            phaseRemainingSec = 0,
            phaseTotalSec = 0,
            totalElapsedSec = 0,
            totalDurationSec = plan.totalDurationSec(),
            isRunning = false
        )
    }
}
