package com.poweder.simpleworkoutlog.ui.interval

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionId: Long? = null  // null = 新規作成、値あり = 編集モード
) {
    val intervalExerciseName by viewModel.intervalExerciseName.collectAsState()
    val editingSession by viewModel.editingSession.collectAsState()

    // 編集モードかどうか
    val isEditMode = sessionId != null

    // タイマー設定
    var settings by remember { mutableStateOf(IntervalTimerSettings.tabataDefault()) }
    var showSettingsDialog by remember { mutableStateOf(!isEditMode) }  // 編集モードでは設定画面をスキップ

    // タイマー状態
    var timerState by remember { mutableStateOf(IntervalTimerState()) }

    // サウンドマネージャー
    val soundManager = remember { IntervalSoundManager() }

    // タイマー開始時刻（SystemClock.elapsedRealtime()ベース）
    var phaseStartTime by remember { mutableStateOf(0L) }
    var phaseDuration by remember { mutableStateOf(0) }
    
    // タイマー全体の開始時刻（総経過時間計算用）
    var totalStartTime by remember { mutableStateOf(0L) }
    // 一時停止時の累積時間
    var pausedTotalElapsed by remember { mutableStateOf(0L) }

    // 完了後の入力画面表示フラグ
    var showResultInput by remember { mutableStateOf(isEditMode) }  // 編集モードでは最初から結果入力画面

    // 消費カロリー入力
    var caloriesInput by remember { mutableStateOf("") }

    // 運動時間入力（編集モード用）
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }

    // 保存完了フラグ
    var hasSaved by remember { mutableStateOf(false) }

    // 重複音防止用：最後にビープを鳴らした残り秒数
    var lastBeepSecond by remember { mutableStateOf(-1) }

    // 編集モード初期化フラグ
    var isEditInitialized by remember { mutableStateOf(false) }

    // 編集モードの場合、セッションをロード
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadIntervalSessionForEdit(sessionId)
        }
    }

    // 編集モードでセッションがロードされたらプリフィル
    LaunchedEffect(editingSession, isEditMode) {
        if (isEditMode && editingSession != null && !isEditInitialized) {
            val session = editingSession!!
            // 秒を分:秒に変換
            val minutes = session.durationSeconds / 60
            val seconds = session.durationSeconds % 60
            durationMinutes = if (minutes > 0) minutes.toString() else ""
            durationSeconds = if (seconds > 0) seconds.toString() else ""
            caloriesInput = if (session.caloriesBurned > 0) session.caloriesBurned.toString() else ""
            isEditInitialized = true
        }
    }

    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    // タイマーループ
    LaunchedEffect(timerState.isRunning, timerState.phase) {
        if (timerState.isRunning && timerState.phase != IntervalTimerPhase.IDLE && timerState.phase != IntervalTimerPhase.FINISHED) {
            while (isActive && timerState.isRunning) {
                val elapsed = ((SystemClock.elapsedRealtime() - phaseStartTime) / 1000).toInt()
                val remaining = (phaseDuration - elapsed).coerceAtLeast(0)

                // 総経過時間を計算（一時停止時の累積 + 現在のセッションの経過）
                val currentTotalElapsed = (pausedTotalElapsed + (SystemClock.elapsedRealtime() - totalStartTime)) / 1000

                // 残り時間を更新
                timerState = timerState.copy(
                    remainingSeconds = remaining,
                    totalElapsedSeconds = currentTotalElapsed.toInt()
                )

                // サウンド再生（残り秒が変わった時のみ鳴らす）
                if (remaining != lastBeepSecond) {
                    when (remaining) {
                        5, 4, 3, 2, 1 -> {
                            soundManager.playShortBeep()
                            lastBeepSecond = remaining
                        }
                        0 -> {
                            soundManager.playLongBeep()
                            lastBeepSecond = remaining
                        }
                    }
                }

                // フェーズ終了
                if (remaining <= 0) {
                    lastBeepSecond = -1
                    moveToNextPhase(
                        currentState = timerState,
                        settings = settings,
                        onStateChange = { newState, newDuration ->
                            timerState = newState
                            phaseDuration = newDuration
                            phaseStartTime = SystemClock.elapsedRealtime()
                        }
                    )
                }

                delay(200L)
            }
        }
    }

    // 設定ダイアログ
    if (showSettingsDialog) {
        IntervalTimerSettingsDialog(
            initialSettings = if (intervalExerciseName == "Tabata") {
                IntervalTimerSettings.tabataDefault()
            } else {
                IntervalTimerSettings.hiitDefault()
            },
            onConfirm = { newSettings ->
                settings = newSettings
                showSettingsDialog = false
                lastBeepSecond = -1
                // タイマー開始時にtotalStartTimeを初期化
                totalStartTime = SystemClock.elapsedRealtime()
                pausedTotalElapsed = 0L
                startTimer(
                    settings = newSettings,
                    onStateChange = { newState, duration ->
                        timerState = newState
                        phaseDuration = duration
                        phaseStartTime = SystemClock.elapsedRealtime()
                    }
                )
            },
            onDismiss = {
                showSettingsDialog = false
                onBack()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // トップバー（種目名のみ表示、戻るボタンなし）
        TopAppBar(
            title = {
                Text(
                    text = intervalExerciseName,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // メインコンテンツ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showSettingsDialog) {
                when {
                    // 結果入力画面
                    showResultInput -> {
                        ResultInputSection(
                            totalSeconds = if (isEditMode) {
                                // 編集モードでは入力値から計算
                                (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                            } else {
                                timerState.totalElapsedSeconds
                            },
                            caloriesInput = caloriesInput,
                            onCaloriesChange = { caloriesInput = it },
                            isEditMode = isEditMode,
                            durationMinutes = durationMinutes,
                            durationSeconds = durationSeconds,
                            onDurationMinutesChange = { durationMinutes = it },
                            onDurationSecondsChange = { durationSeconds = it },
                            onSave = {
                                if (!hasSaved) {
                                    hasSaved = true
                                    val calories = caloriesInput.toIntOrNull() ?: 0

                                    if (isEditMode && sessionId != null) {
                                        // 編集モード：UPDATE
                                        val totalDurationSeconds = (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                                        viewModel.updateIntervalSession(
                                            sessionId = sessionId,
                                            durationSeconds = totalDurationSeconds,
                                            caloriesBurned = calories
                                        )
                                    } else {
                                        // 新規モード：INSERT
                                        viewModel.saveIntervalWorkoutByName(
                                            exerciseName = intervalExerciseName,
                                            durationSeconds = timerState.totalElapsedSeconds,
                                            sets = settings.sets,
                                            caloriesBurned = calories
                                        )
                                    }
                                    viewModel.clearEditingSession()
                                    onBack()
                                }
                            },
                            onCancel = {
                                viewModel.clearEditingSession()
                                onBack()
                            }
                        )
                    }

                    // タイマー完了画面
                    timerState.phase == IntervalTimerPhase.FINISHED -> {
                        FinishedSection(
                            totalSeconds = timerState.totalElapsedSeconds,
                            onComplete = { showResultInput = true }
                        )
                    }

                    // タイマー実行中
                    else -> {
                        // フェーズ表示
                        PhaseIndicator(phase = timerState.phase)

                        Spacer(modifier = Modifier.height(16.dp))

                        // セット表示
                        if (timerState.phase == IntervalTimerPhase.TRAINING || timerState.phase == IntervalTimerPhase.REST) {
                            Text(
                                text = stringResource(R.string.set_progress, timerState.currentSet, timerState.totalSets),
                                style = MaterialTheme.typography.titleLarge,
                                color = WorkoutColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 残り時間（大きく表示）
                        Text(
                            text = formatTime(timerState.remainingSeconds),
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = getPhaseColor(timerState.phase)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // コントロールボタン
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 一時停止/再開ボタン
                            TimerControlButton(
                                text = if (timerState.isRunning) stringResource(R.string.pause) else stringResource(R.string.resume),
                                color = if (timerState.isRunning) WorkoutColors.AccentOrange else WorkoutColors.ButtonConfirm,
                                onClick = {
                                    if (timerState.isRunning) {
                                        // 一時停止時：経過時間を累積に保存
                                        pausedTotalElapsed += SystemClock.elapsedRealtime() - totalStartTime
                                        timerState = timerState.copy(isRunning = false)
                                    } else {
                                        // 再開時：フェーズとトータルの開始時刻を更新
                                        phaseStartTime = SystemClock.elapsedRealtime() - ((phaseDuration - timerState.remainingSeconds) * 1000L)
                                        totalStartTime = SystemClock.elapsedRealtime()
                                        timerState = timerState.copy(isRunning = true)
                                    }
                                }
                            )

                            // 停止ボタン（途中停止 → 完了画面へ）
                            TimerControlButton(
                                text = stringResource(R.string.stop),
                                color = WorkoutColors.PureRed,
                                onClick = {
                                    timerState = timerState.copy(
                                        isRunning = false,
                                        phase = IntervalTimerPhase.FINISHED
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * タイマー完了後の表示
 */
@Composable
private fun FinishedSection(
    totalSeconds: Int,
    onComplete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhaseIndicator(phase = IntervalTimerPhase.FINISHED)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatTime(totalSeconds),
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = getPhaseColor(IntervalTimerPhase.FINISHED)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.workout_complete),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.ButtonConfirm
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.total_time_result, formatTime(totalSeconds)),
            style = MaterialTheme.typography.titleMedium,
            color = WorkoutColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 完了ボタン → 結果入力画面へ
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = WorkoutColors.ButtonConfirm
            )
        ) {
            Text(stringResource(R.string.complete))
        }
    }
}

/**
 * 結果入力画面（運動時間 + 消費カロリー）
 */
@Composable
private fun ResultInputSection(
    totalSeconds: Int,
    caloriesInput: String,
    onCaloriesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    // 編集モード用パラメータ
    isEditMode: Boolean = false,
    durationMinutes: String = "",
    durationSeconds: String = "",
    onDurationMinutesChange: (String) -> Unit = {},
    onDurationSecondsChange: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isEditMode) stringResource(R.string.edit) else stringResource(R.string.save_workout_result),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 運動時間（編集モードでは入力可能）
        if (isEditMode) {
            // 編集モード: 分・秒を入力可能
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { onDurationMinutesChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("分", color = WorkoutColors.TextPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = Color.Black,
                        focusedLabelColor = WorkoutColors.TextPrimary,
                        unfocusedLabelColor = WorkoutColors.TextPrimary
                    )
                )
                Text(":", color = WorkoutColors.TextPrimary)
                OutlinedTextField(
                    value = durationSeconds,
                    onValueChange = { onDurationSecondsChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("秒", color = WorkoutColors.TextPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = Color.Black,
                        focusedLabelColor = WorkoutColors.TextPrimary,
                        unfocusedLabelColor = WorkoutColors.TextPrimary
                    )
                )
            }
        } else {
            // 新規モード: 自動表示（読み取り専用）
            OutlinedTextField(
                value = formatTime(totalSeconds),
                onValueChange = { },
                label = { Text(stringResource(R.string.duration_minutes), color = WorkoutColors.TextPrimary) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary,
                    disabledTextColor = WorkoutColors.TextPrimary,
                    focusedBorderColor = WorkoutColors.AccentOrange,
                    unfocusedBorderColor = Color.Black,
                    focusedLabelColor = WorkoutColors.TextPrimary,
                    unfocusedLabelColor = WorkoutColors.TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 消費カロリー入力
        Column(
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = stringResource(R.string.calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = { onCaloriesChange(it.filter { c -> c.isDigit() }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = Color.Black,
                        focusedPlaceholderColor = WorkoutColors.TextPrimary,
                        unfocusedPlaceholderColor = WorkoutColors.TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.kcal),
                    style = MaterialTheme.typography.titleMedium,
                    color = WorkoutColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ボタン
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // キャンセル
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }

            // 保存
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkoutColors.ButtonConfirm
                )
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun PhaseIndicator(phase: IntervalTimerPhase) {
    val (text, color) = when (phase) {
        IntervalTimerPhase.WARMUP -> stringResource(R.string.phase_warmup) to Color(0xFF4CAF50)
        IntervalTimerPhase.TRAINING -> stringResource(R.string.phase_training) to Color(0xFFFF5722)
        IntervalTimerPhase.REST -> stringResource(R.string.phase_rest) to Color(0xFF2196F3)
        IntervalTimerPhase.COOLDOWN -> stringResource(R.string.phase_cooldown) to Color(0xFF9C27B0)
        IntervalTimerPhase.FINISHED -> stringResource(R.string.phase_finished) to Color(0xFF4CAF50)
        else -> "" to Color.Gray
    }

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TimerControlButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .clickable { onClick() }
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun getPhaseColor(phase: IntervalTimerPhase): Color {
    return when (phase) {
        IntervalTimerPhase.WARMUP -> Color(0xFF4CAF50)
        IntervalTimerPhase.TRAINING -> Color(0xFFFF5722)
        IntervalTimerPhase.REST -> Color(0xFF2196F3)
        IntervalTimerPhase.COOLDOWN -> Color(0xFF9C27B0)
        IntervalTimerPhase.FINISHED -> Color(0xFF4CAF50)
        else -> Color.White
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun startTimer(
    settings: IntervalTimerSettings,
    onStateChange: (IntervalTimerState, Int) -> Unit
) {
    val initialPhase = if (settings.warmupSeconds > 0) {
        IntervalTimerPhase.WARMUP
    } else {
        IntervalTimerPhase.TRAINING
    }

    val initialDuration = if (settings.warmupSeconds > 0) {
        settings.warmupSeconds
    } else {
        settings.trainingSeconds
    }

    val initialSet = if (initialPhase == IntervalTimerPhase.TRAINING) 1 else 0

    onStateChange(
        IntervalTimerState(
            phase = initialPhase,
            currentSet = initialSet,
            totalSets = settings.sets,
            remainingSeconds = initialDuration,
            totalElapsedSeconds = 0,
            isRunning = true
        ),
        initialDuration
    )
}

private fun moveToNextPhase(
    currentState: IntervalTimerState,
    settings: IntervalTimerSettings,
    onStateChange: (IntervalTimerState, Int) -> Unit
) {
    when (currentState.phase) {
        IntervalTimerPhase.WARMUP -> {
            onStateChange(
                currentState.copy(
                    phase = IntervalTimerPhase.TRAINING,
                    currentSet = 1,
                    remainingSeconds = settings.trainingSeconds
                ),
                settings.trainingSeconds
            )
        }

        IntervalTimerPhase.TRAINING -> {
            if (currentState.currentSet >= settings.sets) {
                if (settings.cooldownSeconds > 0) {
                    onStateChange(
                        currentState.copy(
                            phase = IntervalTimerPhase.COOLDOWN,
                            remainingSeconds = settings.cooldownSeconds
                        ),
                        settings.cooldownSeconds
                    )
                } else {
                    onStateChange(
                        currentState.copy(
                            phase = IntervalTimerPhase.FINISHED,
                            isRunning = false
                        ),
                        0
                    )
                }
            } else {
                onStateChange(
                    currentState.copy(
                        phase = IntervalTimerPhase.REST,
                        remainingSeconds = settings.restSeconds
                    ),
                    settings.restSeconds
                )
            }
        }

        IntervalTimerPhase.REST -> {
            onStateChange(
                currentState.copy(
                    phase = IntervalTimerPhase.TRAINING,
                    currentSet = currentState.currentSet + 1,
                    remainingSeconds = settings.trainingSeconds
                ),
                settings.trainingSeconds
            )
        }

        IntervalTimerPhase.COOLDOWN -> {
            onStateChange(
                currentState.copy(
                    phase = IntervalTimerPhase.FINISHED,
                    isRunning = false
                ),
                0
            )
        }

        else -> {}
    }
}