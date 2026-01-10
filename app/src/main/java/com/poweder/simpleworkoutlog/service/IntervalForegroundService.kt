package com.poweder.simpleworkoutlog.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.poweder.simpleworkoutlog.domain.interval.IntervalPhase
import com.poweder.simpleworkoutlog.domain.interval.IntervalPlan
import com.poweder.simpleworkoutlog.domain.interval.IntervalSnapshot
import com.poweder.simpleworkoutlog.domain.interval.IntervalTimerEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * インターバルタイマー Foreground Service
 * 
 * Level C2 仕様：
 * - タイマーの唯一の駆動主体
 * - 画面OFF/他アプリ中でもフェーズ切替＋音が確実に動作
 * - 通知で状況表示（操作ボタンなし）
 * - ラスト5秒カウントダウン（設定ON時）
 */
class IntervalForegroundService : Service() {

    companion object {
        // Intent Actions
        const val ACTION_START = "com.poweder.simpleworkoutlog.interval.START"
        const val ACTION_PAUSE = "com.poweder.simpleworkoutlog.interval.PAUSE"
        const val ACTION_RESUME = "com.poweder.simpleworkoutlog.interval.RESUME"
        const val ACTION_STOP = "com.poweder.simpleworkoutlog.interval.STOP"

        // Intent Extras
        const val EXTRA_PLAN_WARMUP = "plan_warmup"
        const val EXTRA_PLAN_TRAINING = "plan_training"
        const val EXTRA_PLAN_REST = "plan_rest"
        const val EXTRA_PLAN_ROUNDS = "plan_rounds"
        const val EXTRA_PLAN_COOLDOWN = "plan_cooldown"

        // Tick intervals
        private const val TICK_INTERVAL_MS = 500L
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L

        // Timer state SharedPreferences
        private const val PREFS_TIMER_STATE = "interval_timer_prefs"
        private const val KEY_PLAN_WARMUP = "plan_warmup"
        private const val KEY_PLAN_TRAINING = "plan_training"
        private const val KEY_PLAN_REST = "plan_rest"
        private const val KEY_PLAN_ROUNDS = "plan_rounds"
        private const val KEY_PLAN_COOLDOWN = "plan_cooldown"
        private const val KEY_START_MS = "start_ms"
        private const val KEY_PAUSED_ACCUM_MS = "paused_accum_ms"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_PAUSE_START_MS = "pause_start_ms"

        // Settings SharedPreferences (shared with UI)
        const val PREFS_SETTINGS = "swl_prefs"
        const val KEY_TIMER_SOUND = "timer_sound_enabled"
        const val KEY_TIMER_VIBRATION = "timer_vibration_enabled"
        const val KEY_COUNTDOWN_LAST_5 = "countdown_last_5_seconds"
    }

    // Binder for UI connection
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): IntervalForegroundService = this@IntervalForegroundService
    }

    // Timer Engine (Single Source of Truth)
    private var engine: IntervalTimerEngine? = null
    private var plan: IntervalPlan? = null

    // Timer state (managed by Service only)
    private var startMs: Long = 0L
    private var pausedAccumMs: Long = 0L
    private var pauseStartMs: Long = 0L
    private var isRunning: Boolean = false

    // Snapshot for phase change detection
    private var lastSnapshot: IntervalSnapshot? = null
    private var lastNotificationUpdateMs: Long = 0L

    // Countdown state (for preventing duplicate notifications)
    private var lastRemainingSec: Int? = null

    // StateFlow for UI
    private val _snapshotFlow = MutableStateFlow<IntervalSnapshot?>(null)
    val snapshotFlow: StateFlow<IntervalSnapshot?> = _snapshotFlow.asStateFlow()

    // Coroutine scope for tick loop
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickJob: Job? = null

    // Notification helper
    private lateinit var notificationHelper: IntervalNotificationHelper

    // Sound
    private var toneGenerator: ToneGenerator? = null

    // Vibrator
    private var vibrator: Vibrator? = null

    // SharedPreferences
    private lateinit var timerStatePrefs: SharedPreferences
    private lateinit var settingsPrefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        notificationHelper = IntervalNotificationHelper(this)
        timerStatePrefs = getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
        settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)

        // Initialize ToneGenerator
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            // Ignore
        }

        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Try to restore state
        restoreState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val newPlan = IntervalPlan(
                    warmupSec = intent.getIntExtra(EXTRA_PLAN_WARMUP, 10),
                    trainingSec = intent.getIntExtra(EXTRA_PLAN_TRAINING, 20),
                    restSec = intent.getIntExtra(EXTRA_PLAN_REST, 10),
                    rounds = intent.getIntExtra(EXTRA_PLAN_ROUNDS, 8),
                    cooldownSec = intent.getIntExtra(EXTRA_PLAN_COOLDOWN, 30)
                )
                startTimer(newPlan)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        tickJob?.cancel()
        serviceScope.cancel()
        toneGenerator?.release()
        clearSavedState()
    }

    // ===== Settings helpers =====

    private fun isSoundEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_TIMER_SOUND, true)

    private fun isVibrationEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_TIMER_VIBRATION, true)

    private fun isCountdownEnabled(): Boolean =
        settingsPrefs.getBoolean(KEY_COUNTDOWN_LAST_5, false)

    // ===== Timer control =====

    /**
     * タイマー開始
     */
    private fun startTimer(newPlan: IntervalPlan) {
        plan = newPlan
        engine = IntervalTimerEngine(newPlan)

        startMs = SystemClock.elapsedRealtime()
        pausedAccumMs = 0L
        pauseStartMs = 0L
        isRunning = true
        lastSnapshot = null
        lastNotificationUpdateMs = 0L
        lastRemainingSec = null  // Reset countdown state

        // Start foreground with initial notification
        val initialSnapshot = engine!!.snapshot(startMs, startMs, 0L, true)
        _snapshotFlow.value = initialSnapshot

        startForeground(
            IntervalNotificationHelper.NOTIFICATION_ID,
            notificationHelper.createNotification(initialSnapshot)
        )

        // Save state
        saveState()

        // Start tick loop
        startTickLoop()
    }

    /**
     * タイマー一時停止
     */
    private fun pauseTimer() {
        if (!isRunning) return

        pauseStartMs = SystemClock.elapsedRealtime()
        isRunning = false
        lastRemainingSec = null  // Reset to prevent false trigger on resume

        // Update snapshot immediately
        updateSnapshot()

        // Save state
        saveState()

        // Stop tick loop
        tickJob?.cancel()
    }

    /**
     * タイマー再開
     */
    private fun resumeTimer() {
        if (isRunning) return

        // Accumulate paused time
        if (pauseStartMs > 0) {
            pausedAccumMs += SystemClock.elapsedRealtime() - pauseStartMs
            pauseStartMs = 0L
        }

        isRunning = true
        lastRemainingSec = null  // Reset to prevent false trigger

        // Update snapshot immediately
        updateSnapshot()

        // Save state
        saveState()

        // Restart tick loop
        startTickLoop()
    }

    /**
     * タイマー停止
     */
    private fun stopTimer() {
        isRunning = false
        lastRemainingSec = null
        tickJob?.cancel()

        // Send FINISHED snapshot
        plan?.let { p ->
            val finishedSnapshot = IntervalSnapshot(
                phase = IntervalPhase.FINISHED,
                roundIndex = p.rounds,
                totalRounds = p.rounds,
                phaseRemainingSec = 0,
                phaseTotalSec = 0,
                totalElapsedSec = lastSnapshot?.totalElapsedSec ?: 0,
                totalDurationSec = p.totalDurationSec(),
                isRunning = false
            )
            _snapshotFlow.value = finishedSnapshot
        }

        // Clear saved state
        clearSavedState()

        // Stop foreground and service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Tick ループ開始
     */
    private fun startTickLoop() {
        tickJob?.cancel()

        tickJob = serviceScope.launch {
            while (isActive && isRunning) {
                updateSnapshot()
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    /**
     * スナップショット更新
     */
    private fun updateSnapshot() {
        val currentEngine = engine ?: return

        val now = SystemClock.elapsedRealtime()
        val snapshot = currentEngine.snapshot(
            nowMs = now,
            startMs = startMs,
            pausedAccumMs = pausedAccumMs,
            isRunning = isRunning
        )

        // 1. Phase change detection
        maybePlayPhaseChange(snapshot)

        // 2. Countdown (last 5 seconds)
        maybeCountdown(snapshot)

        // 3. Update StateFlow
        _snapshotFlow.value = snapshot
        lastSnapshot = snapshot

        // 4. Update notification (throttled to 1 second)
        if (now - lastNotificationUpdateMs >= NOTIFICATION_UPDATE_INTERVAL_MS) {
            notificationHelper.updateNotification(snapshot)
            lastNotificationUpdateMs = now
        }

        // 5. Check for FINISHED
        if (snapshot.phase == IntervalPhase.FINISHED) {
            onTimerFinished()
        }
    }

    /**
     * フェーズ変更時の処理
     */
    private fun maybePlayPhaseChange(snapshot: IntervalSnapshot) {
        val previousPhase = lastSnapshot?.phase
        if (previousPhase != null && previousPhase != snapshot.phase) {
            when (snapshot.phase) {
                IntervalPhase.TRAINING, IntervalPhase.REST, IntervalPhase.COOLDOWN -> {
                    // Phase change sound/vibration
                    if (isSoundEnabled()) playPhaseChangeBeep()
                    if (isVibrationEnabled()) vibrateOneShot(60)
                }
                IntervalPhase.FINISHED -> {
                    // Handled in onTimerFinished
                }
                else -> {}
            }
        }
    }

    /**
     * ラスト5秒カウントダウン判定
     */
    private fun maybeCountdown(snapshot: IntervalSnapshot) {
        // カウントダウン無効の場合
        if (!isCountdownEnabled()) {
            lastRemainingSec = snapshot.phaseRemainingSec
            return
        }

        // 停止中は鳴らさない
        if (!snapshot.isRunning) {
            lastRemainingSec = snapshot.phaseRemainingSec
            return
        }

        // FINISHED は対象外
        if (snapshot.phase == IntervalPhase.FINISHED) {
            lastRemainingSec = snapshot.phaseRemainingSec
            return
        }

        // WARMUP と COOLDOWN も対象外（WORK/RESTのみ）
        if (snapshot.phase != IntervalPhase.TRAINING && snapshot.phase != IntervalPhase.REST) {
            lastRemainingSec = snapshot.phaseRemainingSec
            return
        }

        val r = snapshot.phaseRemainingSec
        val prev = lastRemainingSec

        // 初回観測は鳴らさない（いきなり5秒域にいるときの誤鳴り防止）
        if (prev == null) {
            lastRemainingSec = r
            return
        }

        // 「秒が変わった」かつ「1..5秒」のときだけ通知
        val shouldNotify = (r in 1..5) && (r != prev)

        if (shouldNotify) {
            if (isVibrationEnabled()) vibrateCountdown()
            if (isSoundEnabled()) playCountdownBeep()
        }

        lastRemainingSec = r
    }

    /**
     * タイマー完了時の処理
     */
    private fun onTimerFinished() {
        // Long beep and vibration for finish
        if (isSoundEnabled()) playFinishBeep()
        if (isVibrationEnabled()) vibrateOneShot(200)

        // Stop tick loop
        tickJob?.cancel()
        isRunning = false

        // Clear saved state
        clearSavedState()

        // Stop foreground
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ===== Sound functions =====

    /**
     * フェーズ切替音（短いビープ）
     */
    private fun playPhaseChangeBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * カウントダウン音（より短いビープ）
     */
    private fun playCountdownBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * 終了音（長いビープ）
     */
    private fun playFinishBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ===== Vibration functions =====

    /**
     * カウントダウンバイブ（短い）
     */
    private fun vibrateCountdown() {
        vibrateOneShot(50)
    }

    /**
     * 汎用バイブレーション
     */
    private fun vibrateOneShot(ms: Long) {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ===== State persistence =====

    /**
     * 状態を保存
     */
    private fun saveState() {
        plan?.let { p ->
            timerStatePrefs.edit().apply {
                putInt(KEY_PLAN_WARMUP, p.warmupSec)
                putInt(KEY_PLAN_TRAINING, p.trainingSec)
                putInt(KEY_PLAN_REST, p.restSec)
                putInt(KEY_PLAN_ROUNDS, p.rounds)
                putInt(KEY_PLAN_COOLDOWN, p.cooldownSec)
                putLong(KEY_START_MS, startMs)
                putLong(KEY_PAUSED_ACCUM_MS, pausedAccumMs)
                putLong(KEY_PAUSE_START_MS, pauseStartMs)
                putBoolean(KEY_IS_RUNNING, isRunning)
                apply()
            }
        }
    }

    /**
     * 状態を復元
     */
    private fun restoreState() {
        if (!timerStatePrefs.contains(KEY_START_MS)) return

        val restoredPlan = IntervalPlan(
            warmupSec = timerStatePrefs.getInt(KEY_PLAN_WARMUP, 10),
            trainingSec = timerStatePrefs.getInt(KEY_PLAN_TRAINING, 20),
            restSec = timerStatePrefs.getInt(KEY_PLAN_REST, 10),
            rounds = timerStatePrefs.getInt(KEY_PLAN_ROUNDS, 8),
            cooldownSec = timerStatePrefs.getInt(KEY_PLAN_COOLDOWN, 30)
        )

        plan = restoredPlan
        engine = IntervalTimerEngine(restoredPlan)
        startMs = timerStatePrefs.getLong(KEY_START_MS, 0L)
        pausedAccumMs = timerStatePrefs.getLong(KEY_PAUSED_ACCUM_MS, 0L)
        pauseStartMs = timerStatePrefs.getLong(KEY_PAUSE_START_MS, 0L)
        isRunning = timerStatePrefs.getBoolean(KEY_IS_RUNNING, false)

        // Calculate current snapshot
        val now = SystemClock.elapsedRealtime()
        val snapshot = engine!!.snapshot(now, startMs, pausedAccumMs, isRunning)

        // Check if already finished
        if (snapshot.phase == IntervalPhase.FINISHED) {
            clearSavedState()
            return
        }

        _snapshotFlow.value = snapshot
        lastSnapshot = snapshot

        // Start foreground if still running
        if (isRunning || pauseStartMs > 0) {
            startForeground(
                IntervalNotificationHelper.NOTIFICATION_ID,
                notificationHelper.createNotification(snapshot)
            )

            if (isRunning) {
                startTickLoop()
            }
        }
    }

    /**
     * 保存状態をクリア
     */
    private fun clearSavedState() {
        timerStatePrefs.edit().clear().apply()
    }
}
