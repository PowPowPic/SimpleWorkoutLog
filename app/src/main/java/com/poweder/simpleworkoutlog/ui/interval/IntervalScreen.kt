package com.poweder.simpleworkoutlog.ui.interval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.domain.interval.IntervalPhase
import com.poweder.simpleworkoutlog.domain.interval.IntervalPlan
import com.poweder.simpleworkoutlog.domain.interval.IntervalSnapshot
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionId: Long? = null  // null = 新規作成、値あり = 編集モード
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val intervalExerciseName by viewModel.intervalExerciseName.collectAsState()
    val editingSession by viewModel.editingSession.collectAsState()
    val plan by viewModel.intervalPlan.collectAsState()
    val showResultInput by viewModel.showIntervalResultInput.collectAsState()

    // Service Connector
    val serviceConnector = remember { IntervalServiceConnector(context) }

    // Service からの snapshot を監視
    var snapshot by remember { mutableStateOf<IntervalSnapshot?>(null) }

    // 編集モードかどうか
    val isEditMode = sessionId != null

    // 設定ダイアログ表示フラグ
    var showSettingsDialog by remember { mutableStateOf(!isEditMode) }

    // 消費カロリー入力
    var caloriesInput by remember { mutableStateOf("") }

    // 運動時間入力（編集モード用）
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }

    // 編集モード初期化フラグ
    var isEditInitialized by remember { mutableStateOf(false) }

    // タイマー開始済みフラグ
    var timerStarted by remember { mutableStateOf(false) }

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
            val minutes = session.durationSeconds / 60
            val seconds = session.durationSeconds % 60
            durationMinutes = if (minutes > 0) minutes.toString() else ""
            durationSeconds = if (seconds > 0) seconds.toString() else ""
            caloriesInput = if (session.caloriesBurned > 0) session.caloriesBurned.toString() else ""
            isEditInitialized = true
        }
    }

    // Lifecycle に応じて Service にバインド/アンバインド
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    serviceConnector.bind()
                }
                Lifecycle.Event.ON_STOP -> {
                    // 画面がバックグラウンドに行ってもServiceは動き続ける
                    // unbindはON_DESTROYで行う
                }
                Lifecycle.Event.ON_DESTROY -> {
                    serviceConnector.unbind()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            serviceConnector.unbind()
        }
    }

    // Service の snapshotFlow を監視
    LaunchedEffect(timerStarted) {
        if (timerStarted) {
            // 少し待ってからバインドを確認
            kotlinx.coroutines.delay(100)
            serviceConnector.snapshotFlow?.collect { newSnapshot ->
                snapshot = newSnapshot
            }
        }
    }

    // 画面離脱時のクリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearIntervalTimer()
        }
    }

    // 設定ダイアログ
    if (showSettingsDialog) {
        IntervalTimerSettingsDialog(
            initialPlan = when (intervalExerciseName) {
                "TABATA" -> IntervalPlan.tabataDefault()
                "HIIT" -> IntervalPlan.hiitDefault()
                "EMOM" -> IntervalPlan.emomDefault()
                else -> IntervalPlan.tabataDefault()
            },
            onConfirm = { newPlan ->
                showSettingsDialog = false
                viewModel.setIntervalPlan(newPlan)
                // Service でタイマーを開始
                serviceConnector.startTimer(newPlan)
                timerStarted = true
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
        // トップバー
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
                    showResultInput || isEditMode -> {
                        ResultInputSection(
                            totalSeconds = if (isEditMode) {
                                (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                            } else {
                                snapshot?.totalElapsedSec ?: 0
                            },
                            caloriesInput = caloriesInput,
                            onCaloriesChange = { caloriesInput = it },
                            isEditMode = isEditMode,
                            durationMinutes = durationMinutes,
                            durationSeconds = durationSeconds,
                            onDurationMinutesChange = { durationMinutes = it },
                            onDurationSecondsChange = { durationSeconds = it },
                            onCancel = {
                                serviceConnector.stopTimer()
                                viewModel.clearIntervalTimer()
                                viewModel.clearEditingSession()
                                onBack()
                            },
                            onSave = {
                                val totalDurationSeconds = if (isEditMode) {
                                    (durationMinutes.toIntOrNull() ?: 0) * 60 + (durationSeconds.toIntOrNull() ?: 0)
                                } else {
                                    snapshot?.totalElapsedSec ?: 0
                                }
                                val calories = caloriesInput.toIntOrNull() ?: 0

                                if (isEditMode && sessionId != null) {
                                    viewModel.updateIntervalSession(
                                        sessionId = sessionId,
                                        durationSeconds = totalDurationSeconds,
                                        caloriesBurned = calories
                                    )
                                } else {
                                    viewModel.saveIntervalWorkoutByName(
                                        exerciseName = intervalExerciseName,
                                        durationSeconds = totalDurationSeconds,
                                        sets = plan?.rounds ?: 0,
                                        caloriesBurned = calories
                                    )
                                }
                                viewModel.clearIntervalTimer()
                                onBack()
                            }
                        )
                    }

                    // タイマー動作中
                    snapshot != null -> {
                        val currentSnapshot = snapshot!!

                        when (currentSnapshot.phase) {
                            IntervalPhase.FINISHED -> {
                                // 完了表示
                                FinishedSection(
                                    totalSeconds = currentSnapshot.totalElapsedSec,
                                    onComplete = {
                                        viewModel.showIntervalResultInput()
                                    }
                                )
                            }

                            else -> {
                                // タイマー表示
                                TimerSection(
                                    snapshot = currentSnapshot,
                                    onPauseResume = {
                                        if (currentSnapshot.isRunning) {
                                            serviceConnector.pauseTimer()
                                        } else {
                                            serviceConnector.resumeTimer()
                                        }
                                    },
                                    onStop = {
                                        serviceConnector.stopTimer()
                                        viewModel.showIntervalResultInput()
                                    }
                                )
                            }
                        }
                    }

                    // タイマー開始前（設定後、Serviceからのsnapshot待ち）
                    timerStarted -> {
                        CircularProgressIndicator(
                            color = WorkoutColors.AccentOrange
                        )
                    }
                }
            }
        }

        // システムナビゲーションバー用のスペース
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * タイマー表示セクション
 */
@Composable
private fun TimerSection(
    snapshot: IntervalSnapshot,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // フェーズ表示
        PhaseIndicator(phase = snapshot.phase)

        Spacer(modifier = Modifier.height(8.dp))

        // セット表示
        if (snapshot.phase == IntervalPhase.TRAINING || snapshot.phase == IntervalPhase.REST) {
            Text(
                text = stringResource(R.string.set_progress, snapshot.roundIndex, snapshot.totalRounds),
                style = MaterialTheme.typography.titleLarge,
                color = WorkoutColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 残り時間（大きく表示）
        Text(
            text = formatTime(snapshot.phaseRemainingSec),
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = getPhaseColor(snapshot.phase)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 総経過時間
        Text(
            text = stringResource(R.string.total_elapsed, formatTime(snapshot.totalElapsedSec)),
            style = MaterialTheme.typography.titleMedium,
            color = WorkoutColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // コントロールボタン
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 一時停止/再開ボタン
            Button(
                onClick = onPauseResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (snapshot.isRunning) WorkoutColors.ButtonCancel else WorkoutColors.ButtonConfirm
                )
            ) {
                Text(
                    text = if (snapshot.isRunning) {
                        stringResource(R.string.pause)
                    } else {
                        stringResource(R.string.resume)
                    }
                )
            }

            // 停止ボタン
            OutlinedButton(onClick = onStop) {
                Text(stringResource(R.string.stop))
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
        PhaseIndicator(phase = IntervalPhase.FINISHED)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatTime(totalSeconds),
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = getPhaseColor(IntervalPhase.FINISHED)
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
 * 結果入力画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultInputSection(
    totalSeconds: Int,
    caloriesInput: String,
    onCaloriesChange: (String) -> Unit,
    isEditMode: Boolean,
    durationMinutes: String,
    durationSeconds: String,
    onDurationMinutesChange: (String) -> Unit,
    onDurationSecondsChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isEditMode) {
                stringResource(R.string.edit_interval_result)
            } else {
                stringResource(R.string.input_result)
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 運動時間
        if (isEditMode) {
            Text(
                text = stringResource(R.string.duration),
                style = MaterialTheme.typography.titleMedium,
                color = WorkoutColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { onDurationMinutesChange(it.filter { c -> c.isDigit() }) },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = Color.Black,
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        cursorColor = WorkoutColors.AccentOrange
                    )
                )
                Text(
                    text = "分",
                    style = MaterialTheme.typography.titleMedium,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                OutlinedTextField(
                    value = durationSeconds,
                    onValueChange = { onDurationSecondsChange(it.filter { c -> c.isDigit() }) },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = Color.Black,
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        cursorColor = WorkoutColors.AccentOrange
                    )
                )
                Text(
                    text = "秒",
                    style = MaterialTheme.typography.titleMedium,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            Text(
                text = stringResource(R.string.duration),
                style = MaterialTheme.typography.titleMedium,
                color = WorkoutColors.TextSecondary
            )
            Text(
                text = formatTime(totalSeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.AccentOrange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 消費カロリー入力
        Column {
            Text(
                text = stringResource(R.string.exercise_calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = { onCaloriesChange(it.filter { c -> c.isDigit() }) },
                    modifier = Modifier.width(200.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = Color.Black,
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        cursorColor = WorkoutColors.AccentOrange
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "kcal",
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
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.common_cancel))
            }

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
private fun PhaseIndicator(phase: IntervalPhase) {
    val (text, color) = when (phase) {
        IntervalPhase.WARMUP -> stringResource(R.string.phase_warmup) to Color(0xFF4CAF50)
        IntervalPhase.TRAINING -> stringResource(R.string.phase_training) to Color(0xFFFF5722)
        IntervalPhase.REST -> stringResource(R.string.phase_rest) to Color(0xFF2196F3)
        IntervalPhase.COOLDOWN -> stringResource(R.string.phase_cooldown) to Color(0xFF9C27B0)
        IntervalPhase.FINISHED -> stringResource(R.string.phase_finished) to WorkoutColors.ButtonConfirm
        IntervalPhase.IDLE -> "" to Color.Transparent
    }

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun getPhaseColor(phase: IntervalPhase): Color {
    return when (phase) {
        IntervalPhase.WARMUP -> Color(0xFF4CAF50)
        IntervalPhase.TRAINING -> Color(0xFFFF5722)
        IntervalPhase.REST -> Color(0xFF2196F3)
        IntervalPhase.COOLDOWN -> Color(0xFF9C27B0)
        IntervalPhase.FINISHED -> Color(0xFF4CAF50)
        IntervalPhase.IDLE -> WorkoutColors.TextPrimary
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

// ========== 設定ダイアログ ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalTimerSettingsDialog(
    initialPlan: IntervalPlan,
    onConfirm: (IntervalPlan) -> Unit,
    onDismiss: () -> Unit
) {
    var warmupSec by remember { mutableStateOf(initialPlan.warmupSec.toString()) }
    var trainingSec by remember { mutableStateOf(initialPlan.trainingSec.toString()) }
    var restSec by remember { mutableStateOf(initialPlan.restSec.toString()) }
    var rounds by remember { mutableStateOf(initialPlan.rounds.toString()) }
    var cooldownSec by remember { mutableStateOf(initialPlan.cooldownSec.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.timer_settings),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsRow(
                    label = stringResource(R.string.warmup_seconds),
                    value = warmupSec,
                    onValueChange = { warmupSec = it },
                    suffix = stringResource(R.string.seconds_unit)
                )

                SettingsRow(
                    label = stringResource(R.string.training_seconds),
                    value = trainingSec,
                    onValueChange = { trainingSec = it },
                    suffix = stringResource(R.string.seconds_unit)
                )

                SettingsRow(
                    label = stringResource(R.string.rest_seconds),
                    value = restSec,
                    onValueChange = { restSec = it },
                    suffix = stringResource(R.string.seconds_unit)
                )

                SettingsRow(
                    label = stringResource(R.string.sets_count),
                    value = rounds,
                    onValueChange = { rounds = it },
                    suffix = stringResource(R.string.sets_unit)
                )

                SettingsRow(
                    label = stringResource(R.string.cooldown_seconds),
                    value = cooldownSec,
                    onValueChange = { cooldownSec = it },
                    suffix = stringResource(R.string.seconds_unit)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val totalSeconds = calculateTotalTime(
                    warmupSec.toIntOrNull() ?: 0,
                    trainingSec.toIntOrNull() ?: 0,
                    restSec.toIntOrNull() ?: 0,
                    rounds.toIntOrNull() ?: 0,
                    cooldownSec.toIntOrNull() ?: 0
                )
                Text(
                    text = stringResource(R.string.total_time_estimate, formatTime(totalSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val plan = IntervalPlan(
                        warmupSec = warmupSec.toIntOrNull() ?: 10,
                        trainingSec = trainingSec.toIntOrNull() ?: 20,
                        restSec = restSec.toIntOrNull() ?: 10,
                        rounds = rounds.toIntOrNull() ?: 8,
                        cooldownSec = cooldownSec.toIntOrNull() ?: 30
                    )
                    onConfirm(plan)
                }
            ) {
                Text(stringResource(R.string.start), color = WorkoutColors.AccentOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WorkoutColors.AccentOrange,
                    unfocusedBorderColor = Color.Black,
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = suffix,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun calculateTotalTime(
    warmup: Int,
    training: Int,
    rest: Int,
    rounds: Int,
    cooldown: Int
): Int {
    if (rounds <= 0) return 0
    var total = warmup
    total += training * rounds + rest * (rounds - 1).coerceAtLeast(0)
    total += cooldown
    return total
}
