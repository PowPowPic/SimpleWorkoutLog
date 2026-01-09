package com.poweder.simpleworkoutlog.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import com.poweder.simpleworkoutlog.ui.components.DurationInputField
import com.poweder.simpleworkoutlog.ui.components.durationToSeconds
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionId: Long? = null  // null = 新規作成、値あり = 編集モード
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adRemoved by viewModel.adRemoved.collectAsState()
    val currentExercise by viewModel.currentStudioExercise.collectAsState()
    val editingSession by viewModel.editingSession.collectAsState()

    // 編集モードかどうか
    val isEditMode = sessionId != null

    // 入力値（運動時間は時間・分・秒の3フィールド）
    var durationHours by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    // 編集モード初期化フラグ
    var isEditInitialized by remember { mutableStateOf(false) }

    // 編集モードの場合、セッションをロード
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadStudioSessionForEdit(sessionId)
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
            calories = if (session.caloriesBurned > 0) session.caloriesBurned.toString() else ""
            isEditInitialized = true
        }
    }

    // 未保存確認ダイアログ
    var showBackConfirmDialog by remember { mutableStateOf(false) }

    // 日付をライフサイクルに連動して更新
    var logicalDate by remember { mutableStateOf(currentLogicalDate()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                logicalDate = currentLogicalDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
    }

    // 入力があるかどうか
    val hasInput = durationHours.isNotBlank() || durationMinutes.isNotBlank() || durationSeconds.isNotBlank() || calories.isNotBlank()

    // 未保存確認ダイアログ
    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = { Text(stringResource(R.string.discard_confirm_title)) },
            text = { Text(stringResource(R.string.discard_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackConfirmDialog = false
                        viewModel.clearEditingSession()
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.discard), color = WorkoutColors.PureRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    val backgroundGradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.StudioCardStart,
            WorkoutColors.StudioCardEnd
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // 広告バナー
        TopBannerAd(showAd = !adRemoved)

        // 日付表示
        Text(
            text = logicalDate.format(dateFormatter),
            style = MaterialTheme.typography.bodySmall,
            color = WorkoutColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // 種目名
        Text(
            text = currentExercise?.getDisplayName(context) ?: "Studio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 入力エリア
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 時間（時間・分・秒の3フィールド）
            DurationInputField(
                hours = durationHours,
                minutes = durationMinutes,
                seconds = durationSeconds,
                onHoursChange = { durationHours = it },
                onMinutesChange = { durationMinutes = it },
                onSecondsChange = { durationSeconds = it },
                modifier = Modifier.fillMaxWidth()
            )

            // 消費カロリー
            InputField(
                label = stringResource(R.string.calories),
                value = calories,
                onValueChange = { calories = it },
                suffix = stringResource(R.string.kcal)
            )
        }

        // 下部ボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Home ボタン
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.ButtonCancel)
                    .clickable {
                        if (hasInput) {
                            showBackConfirmDialog = true
                        } else {
                            onBack()
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.go_home),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            }

            // Save ボタン
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.ButtonConfirm)
                    .clickable {
                        // 運動時間を秒に変換
                        val totalDurationSeconds = durationToSeconds(durationHours, durationMinutes, durationSeconds)
                        val cal = calories.toIntOrNull() ?: 0

                        if (isEditMode && sessionId != null) {
                            // 編集モード：UPDATE
                            viewModel.updateStudioSession(
                                sessionId = sessionId,
                                durationSeconds = totalDurationSeconds,
                                caloriesBurned = cal
                            )
                        } else {
                            // 新規モード：INSERT
                            currentExercise?.let { exercise ->
                                viewModel.saveStudioWorkout(
                                    exerciseId = exercise.id,
                                    durationSeconds = totalDurationSeconds,
                                    caloriesBurned = cal
                                )
                            }
                        }
                        onBack()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.finish_and_save),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    Column {
        Text(
            text = label,
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
                value = value,
                onValueChange = { newValue ->
                    // 数字のみ許可
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WorkoutColors.AccentOrange,
                    unfocusedBorderColor = WorkoutColors.TextSecondary,
                    cursorColor = WorkoutColors.AccentOrange,
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = suffix,
                style = MaterialTheme.typography.titleMedium,
                color = WorkoutColors.TextSecondary
            )
        }
    }
}