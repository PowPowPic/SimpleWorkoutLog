package com.poweder.simpleworkoutlog.ui.interval

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentExercise by viewModel.currentExercise.collectAsState()
    
    // タイマー設定
    var settings by remember { mutableStateOf(IntervalTimerSettings.tabataDefault()) }
    var showSettingsDialog by remember { mutableStateOf(true) }
    
    // タイマー状態
    var timerState by remember { mutableStateOf(IntervalTimerState()) }
    
    // サウンドマネージャー
    val soundManager = remember { IntervalSoundManager() }
    
    // タイマー開始時刻（SystemClock.elapsedRealtime()ベース）
    var phaseStartTime by remember { mutableStateOf(0L) }
    var phaseDuration by remember { mutableStateOf(0) }
    
    // 完了時の保存フラグ
    var hasSaved by remember { mutableStateOf(false) }
    
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
                
                // 残り時間を更新
                timerState = timerState.copy(
                    remainingSeconds = remaining,
                    totalElapsedSeconds = timerState.totalElapsedSeconds + 1
                )
                
                // サウンド再生（残り5秒から）
                if (remaining in 1..5) {
                    soundManager.playShortBeep()
                }
                
                // フェーズ終了
                if (remaining <= 0) {
                    soundManager.playLongBeep()
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
                
                delay(1000L)
            }
        }
    }
    
    // 設定ダイアログ
    if (showSettingsDialog) {
        IntervalTimerSettingsDialog(
            initialSettings = settings,
            onConfirm = { newSettings ->
                settings = newSettings
                showSettingsDialog = false
                // タイマー開始
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
    
    // 完了時の保存処理
    LaunchedEffect(timerState.phase) {
        if (timerState.phase == IntervalTimerPhase.FINISHED && !hasSaved) {
            hasSaved = true
            currentExercise?.let { exercise ->
                viewModel.saveIntervalWorkout(
                    exerciseId = exercise.id,
                    durationSeconds = timerState.totalElapsedSeconds,
                    sets = settings.sets
                )
            }
        }
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
                    text = currentExercise?.getDisplayName(context) ?: stringResource(R.string.workout_interval),
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (timerState.isRunning) {
                        // タイマー実行中は確認なしで停止して戻る
                        timerState = timerState.copy(isRunning = false)
                    }
                    onBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = WorkoutColors.TextPrimary
                    )
                }
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
                when (timerState.phase) {
                    IntervalTimerPhase.FINISHED -> {
                        // 完了画面
                        Text(
                            text = stringResource(R.string.workout_complete),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutColors.ButtonConfirm
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.total_time_result, formatTime(timerState.totalElapsedSeconds)),
                            style = MaterialTheme.typography.titleMedium,
                            color = WorkoutColors.TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorkoutColors.ButtonConfirm
                            )
                        ) {
                            Text(stringResource(R.string.back_to_home))
                        }
                    }
                    
                    IntervalTimerPhase.IDLE -> {
                        // 待機中（設定ダイアログ表示後）
                    }
                    
                    else -> {
                        // 実行中
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 一時停止/再開ボタン
                            TimerControlButton(
                                text = if (timerState.isRunning) stringResource(R.string.pause) else stringResource(R.string.resume),
                                color = if (timerState.isRunning) WorkoutColors.AccentOrange else WorkoutColors.ButtonConfirm,
                                onClick = {
                                    if (timerState.isRunning) {
                                        // 一時停止
                                        timerState = timerState.copy(isRunning = false)
                                    } else {
                                        // 再開
                                        phaseStartTime = SystemClock.elapsedRealtime() - ((phaseDuration - timerState.remainingSeconds) * 1000L)
                                        timerState = timerState.copy(isRunning = true)
                                    }
                                }
                            )
                            
                            // 停止ボタン
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
            // ウォームアップ → トレーニング
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
                // 最後のセット → クールダウン or 完了
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
                // トレーニング → インターバル
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
            // インターバル → 次のトレーニング
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
            // クールダウン → 完了
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
