package com.poweder.simpleworkoutlog.domain.interval

/**
 * インターバルタイマーの純ロジックエンジン
 * 
 * 設計思想：
 * - Compose / ViewModel / Service など環境依存コードを入れない
 * - nowMs（現在時刻）を外から注入して計算する（= テスト可能）
 * - 「今どのフェーズで残り何秒か」を算出することが本体
 * 
 * 将来のForegroundService対応時も、このEngineをそのまま使える
 */
class IntervalTimerEngine(private val plan: IntervalPlan) {

    /**
     * 現在のスナップショットを計算
     * 
     * @param nowMs 現在時刻（SystemClock.elapsedRealtime()など）
     * @param startMs タイマー開始時刻
     * @param pausedAccumMs 一時停止中に累積した時間（ミリ秒）
     * @param isRunning タイマーが動作中か
     * @return 現在の状態スナップショット
     */
    fun snapshot(
        nowMs: Long,
        startMs: Long,
        pausedAccumMs: Long,
        isRunning: Boolean
    ): IntervalSnapshot {
        // タイマーが開始されていない場合
        if (startMs == 0L) {
            return IntervalSnapshot.idle(plan)
        }

        // 経過時間を計算（一時停止中の時間を除く）
        val elapsedMs = if (isRunning) {
            nowMs - startMs - pausedAccumMs
        } else {
            // 一時停止中は、pausedAccumMsに「開始から一時停止までの経過」が入っている想定ではなく、
            // 「累積した一時停止時間」なので、最後のアクティブ時間を別途計算する必要がある
            // ただし、この設計では一時停止時はnowMsを更新しないので、最後のnowMsで計算
            nowMs - startMs - pausedAccumMs
        }

        val elapsedSec = (elapsedMs / 1000).toInt().coerceAtLeast(0)
        val totalDuration = plan.totalDurationSec()

        // 完了判定
        if (elapsedSec >= totalDuration) {
            return IntervalSnapshot(
                phase = IntervalPhase.FINISHED,
                roundIndex = plan.rounds,
                totalRounds = plan.rounds,
                phaseRemainingSec = 0,
                phaseTotalSec = 0,
                totalElapsedSec = totalDuration,
                totalDurationSec = totalDuration,
                isRunning = false
            )
        }

        // 現在のフェーズを経過時間から決定
        return calculatePhaseFromElapsed(elapsedSec, isRunning)
    }

    /**
     * 経過秒数から現在のフェーズを計算
     */
    private fun calculatePhaseFromElapsed(elapsedSec: Int, isRunning: Boolean): IntervalSnapshot {
        var currentSec = elapsedSec
        val totalDuration = plan.totalDurationSec()

        // 1. WARMUP フェーズ
        if (plan.warmupSec > 0 && currentSec < plan.warmupSec) {
            return IntervalSnapshot(
                phase = IntervalPhase.WARMUP,
                roundIndex = 0,
                totalRounds = plan.rounds,
                phaseRemainingSec = plan.warmupSec - currentSec,
                phaseTotalSec = plan.warmupSec,
                totalElapsedSec = elapsedSec,
                totalDurationSec = totalDuration,
                isRunning = isRunning
            )
        }

        // WARMUPを超えた分
        currentSec -= plan.warmupSec

        // 2. TRAINING/REST サイクル
        for (round in 1..plan.rounds) {
            // TRAINING フェーズ
            if (currentSec < plan.trainingSec) {
                return IntervalSnapshot(
                    phase = IntervalPhase.TRAINING,
                    roundIndex = round,
                    totalRounds = plan.rounds,
                    phaseRemainingSec = plan.trainingSec - currentSec,
                    phaseTotalSec = plan.trainingSec,
                    totalElapsedSec = elapsedSec,
                    totalDurationSec = totalDuration,
                    isRunning = isRunning
                )
            }
            currentSec -= plan.trainingSec

            // REST フェーズ（最終ラウンド以外）
            if (round < plan.rounds) {
                if (currentSec < plan.restSec) {
                    return IntervalSnapshot(
                        phase = IntervalPhase.REST,
                        roundIndex = round,
                        totalRounds = plan.rounds,
                        phaseRemainingSec = plan.restSec - currentSec,
                        phaseTotalSec = plan.restSec,
                        totalElapsedSec = elapsedSec,
                        totalDurationSec = totalDuration,
                        isRunning = isRunning
                    )
                }
                currentSec -= plan.restSec
            }
        }

        // 3. COOLDOWN フェーズ
        if (plan.cooldownSec > 0 && currentSec < plan.cooldownSec) {
            return IntervalSnapshot(
                phase = IntervalPhase.COOLDOWN,
                roundIndex = plan.rounds,
                totalRounds = plan.rounds,
                phaseRemainingSec = plan.cooldownSec - currentSec,
                phaseTotalSec = plan.cooldownSec,
                totalElapsedSec = elapsedSec,
                totalDurationSec = totalDuration,
                isRunning = isRunning
            )
        }

        // 4. FINISHED
        return IntervalSnapshot(
            phase = IntervalPhase.FINISHED,
            roundIndex = plan.rounds,
            totalRounds = plan.rounds,
            phaseRemainingSec = 0,
            phaseTotalSec = 0,
            totalElapsedSec = totalDuration,
            totalDurationSec = totalDuration,
            isRunning = false
        )
    }

    /**
     * 総トレーニング時間（秒）
     */
    fun totalDurationSec(): Int = plan.totalDurationSec()

    /**
     * プランを取得
     */
    fun getPlan(): IntervalPlan = plan
}
