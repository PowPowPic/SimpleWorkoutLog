package com.poweder.simpleworkoutlog.ui.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun OtherScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionId: Long? = null  // null = 新規作成、値あり = 編集モード
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adRemoved by viewModel.adRemoved.collectAsState()
    val currentExercise by viewModel.currentOtherExercise.collectAsState()
    val editingSession by viewModel.editingSession.collectAsState()

    // 編集モードかどうか
    val isEditMode = sessionId != null

    // 運動時間は時間・分・秒の3フィールド
    var durationHours by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }
    var caloriesBurned by remember { mutableStateOf("") }

    // 未保存確認ダイアログ
    var showBackConfirmDialog by remember { mutableStateOf(false) }

    // 編集モード初期化フラグ
    var isEditInitialized by remember { mutableStateOf(false) }

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

    // 入力があるかどうか
    val hasInput = durationHours.isNotBlank() || durationMinutes.isNotBlank() || 
                   durationSeconds.isNotBlank() || caloriesBurned.isNotBlank()

    // カードのグラデーション
    val backgroundGradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.OtherCardStart,
            WorkoutColors.OtherCardEnd
        )
    )

    // 戻る確認ダイアログ
    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_back_title)) },
            text = { Text(stringResource(R.string.confirm_back_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackConfirmDialog = false
                        viewModel.clearEditingSession()
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

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
            text = currentExercise?.getDisplayName(context) ?: stringResource(R.string.workout_other),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // スクロール可能なコンテンツエリア（高齢者対応）
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            // 消費カロリー入力
            Column {
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
                        value = caloriesBurned,
                        onValueChange = { caloriesBurned = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WorkoutColors.AccentOrange,
                            unfocusedBorderColor = Color.Black,
                            cursorColor = WorkoutColors.AccentOrange,
                            focusedTextColor = WorkoutColors.TextPrimary,
                            unfocusedTextColor = WorkoutColors.TextPrimary
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
                    .heightIn(min = 48.dp)  // 最低タップサイズ保証（高齢者対応）
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.ButtonCancel)
                    .clickable {
                        if (hasInput) {
                            showBackConfirmDialog = true
                        } else {
                            viewModel.clearEditingSession()
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
                    color = WorkoutColors.TextPrimary,
                    maxLines = 2  // 高齢者対応
                )
            }

            // Save ボタン
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)  // 最低タップサイズ保証（高齢者対応）
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.ButtonConfirm)
                    .clickable {
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
                                // 入力をクリア
                                durationHours = ""
                                durationMinutes = ""
                                durationSeconds = ""
                                caloriesBurned = ""
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.finish_and_save),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary,
                    maxLines = 2  // 高齢者対応
                )
            }
        }

        // システムナビゲーションバー用のスペース
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
