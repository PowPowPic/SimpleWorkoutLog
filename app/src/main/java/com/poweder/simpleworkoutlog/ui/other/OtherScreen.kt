package com.poweder.simpleworkoutlog.ui.other

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.ui.components.DurationInputField
import com.poweder.simpleworkoutlog.ui.components.durationToSeconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionId: Long? = null  // null = 新規作成、値あり = 編集モード
) {
    val context = LocalContext.current
    val currentExercise by viewModel.currentOtherExercise.collectAsState()
    val editingSession by viewModel.editingSession.collectAsState()

    // 編集モードかどうか
    val isEditMode = sessionId != null

    // 運動時間は時間・分・秒の3フィールド
    var durationHours by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }
    var caloriesBurned by remember { mutableStateOf("") }
    var showSavedMessage by remember { mutableStateOf(false) }

    // 編集モード初期化フラグ
    var isEditInitialized by remember { mutableStateOf(false) }

    // 編集モードの場合、セッションをロード
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadOtherSessionForEdit(sessionId)
        }
    }

    // 編集モードでセッションがロードされたらプリフィル
    LaunchedEffect(editingSession, isEditMode) {
        if (isEditMode && editingSession != null && !isEditInitialized) {
            val session = editingSession!!
            // 秒を時:分:秒に変換
            val hours = session.durationSeconds / 3600
            val minutes = (session.durationSeconds % 3600) / 60
            val seconds = session.durationSeconds % 60
            durationHours = if (hours > 0) hours.toString() else ""
            durationMinutes = if (minutes > 0) minutes.toString() else ""
            durationSeconds = if (seconds > 0) seconds.toString() else ""
            caloriesBurned = if (session.caloriesBurned > 0) session.caloriesBurned.toString() else ""
            isEditInitialized = true
        }
    }

    // カードのグラデーション
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.OtherCardStart,
            WorkoutColors.OtherCardEnd
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // トップバー
        TopAppBar(
            title = {
                Text(
                    text = currentExercise?.getDisplayName(context) ?: stringResource(R.string.workout_other),
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.clearEditingSession()
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 入力カード
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardGradient)
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 種目名
                    Text(
                        text = currentExercise?.getDisplayName(context) ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 時間入力（時間・分・秒の3フィールド）
                    DurationInputField(
                        hours = durationHours,
                        minutes = durationMinutes,
                        seconds = durationSeconds,
                        onHoursChange = { durationHours = it },
                        onMinutesChange = { durationMinutes = it },
                        onSecondsChange = { durationSeconds = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 消費カロリー入力
                    OutlinedTextField(
                        value = caloriesBurned,
                        onValueChange = { caloriesBurned = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.calories_burned)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WorkoutColors.TextPrimary,
                            unfocusedBorderColor = WorkoutColors.TextSecondary,
                            focusedLabelColor = WorkoutColors.TextPrimary,
                            unfocusedLabelColor = WorkoutColors.TextSecondary,
                            focusedTextColor = WorkoutColors.TextPrimary,
                            unfocusedTextColor = WorkoutColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存ボタン
            Button(
                onClick = {
                    // 運動時間を秒に変換
                    val totalDurationSeconds = durationToSeconds(durationHours, durationMinutes, durationSeconds)
                    val calories = caloriesBurned.toIntOrNull() ?: 0

                    if (isEditMode && sessionId != null) {
                        // 編集モード：UPDATE
                        viewModel.updateOtherSession(
                            sessionId = sessionId,
                            durationSeconds = totalDurationSeconds,
                            caloriesBurned = calories
                        )
                        onBack()
                    } else {
                        // 新規モード：INSERT
                        currentExercise?.let { exercise ->
                            viewModel.saveOtherWorkout(
                                exerciseId = exercise.id,
                                durationSeconds = totalDurationSeconds,
                                caloriesBurned = calories
                            )
                            showSavedMessage = true
                            durationHours = ""
                            durationMinutes = ""
                            durationSeconds = ""
                            caloriesBurned = ""
                        }
                    }
                },
                enabled = durationHours.isNotBlank() || durationMinutes.isNotBlank() || durationSeconds.isNotBlank() || caloriesBurned.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkoutColors.ButtonConfirm
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 保存完了メッセージ
            if (showSavedMessage) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.saved_successfully),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.ButtonConfirm,
                    textAlign = TextAlign.Center
                )

                LaunchedEffect(showSavedMessage) {
                    kotlinx.coroutines.delay(2000)
                    showSavedMessage = false
                }
            }
        }
    }
}