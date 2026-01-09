package com.poweder.simpleworkoutlog.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.WorkoutDayDetailDialog
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import com.poweder.simpleworkoutlog.util.formatHms
import com.poweder.simpleworkoutlog.util.formatHms
import com.poweder.simpleworkoutlog.util.formatWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onNavigateToStrengthEdit: (Long) -> Unit = {},
    onNavigateToCardioEdit: (Long) -> Unit = {},
    onNavigateToIntervalEdit: (Long) -> Unit = {},
    onNavigateToStudioEdit: (Long) -> Unit = {},
    onNavigateToOtherEdit: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val weightUnit by viewModel.weightUnit.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val adRemoved by viewModel.adRemoved.collectAsState()

    // 選択中の日付
    var selectedDate by remember { mutableStateOf(currentLogicalDate()) }

    // 選択日のセッション
    val sessionsForDate by viewModel.getSessionsForDate(selectedDate).collectAsState(initial = emptyList())

    // 選択日のセット（重量計算用）
    val setsForDate by viewModel.getSetsForDate(selectedDate).collectAsState(initial = emptyList())

    // 全種目リスト
    val allExercises by viewModel.allExercises.collectAsState()

    // 種目名マップを作成（getDisplayNameを使用してnameResIdに対応）
    val exerciseNames = remember(allExercises, context) {
        allExercises.associate { exercise ->
            exercise.id to exercise.getDisplayName(context)
        }
    }

    // セッションIDごとの総重量を計算
    val sessionWeights = remember(setsForDate) {
        setsForDate.groupBy { it.sessionId }
            .mapValues { (_, sets) -> sets.sumOf { it.weight * it.reps } }
    }

    // 詳細ダイアログ表示フラグ
    var showDetailDialog by remember { mutableStateOf(false) }

    // 削除確認ダイアログ
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
    }

    // カテゴリ別にグループ化
    val sessionsByType = sessionsForDate.groupBy { it.workoutType }

    // 合計計算（セットから重量を計算）- 秒で計算
    val totalDurationSeconds = sessionsForDate.sumOf { it.durationSeconds }
    val totalCalories = sessionsForDate.sumOf { it.caloriesBurned }
    val totalWeight = setsForDate.sumOf { it.weight * it.reps }

    // カテゴリ削除確認ダイアログ
    if (showDeleteCategoryDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteCategoryDialog = false
                categoryToDelete = null
            },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { type ->
                            sessionsByType[type]?.forEach { session ->
                                viewModel.deleteSession(session.id)
                            }
                        }
                        showDeleteCategoryDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.common_ok), color = WorkoutColors.PureRed)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteCategoryDialog = false
                    categoryToDelete = null
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 詳細ダイアログ
    if (showDetailDialog && sessionsForDate.isNotEmpty()) {
        WorkoutDayDetailDialog(
            date = selectedDate,
            sessions = sessionsForDate,
            exerciseNames = exerciseNames,
            sessionWeights = sessionWeights,
            weightUnit = weightUnit,
            distanceUnit = distanceUnit,
            onEditSession = { session ->
                // workoutTypeに応じて編集画面へ遷移
                showDetailDialog = false
                when (session.workoutType) {
                    WorkoutType.STRENGTH -> onNavigateToStrengthEdit(session.id)
                    WorkoutType.CARDIO -> onNavigateToCardioEdit(session.id)
                    WorkoutType.INTERVAL -> onNavigateToIntervalEdit(session.id)
                    WorkoutType.STUDIO -> onNavigateToStudioEdit(session.id)
                    WorkoutType.OTHER -> onNavigateToOtherEdit(session.id)
                }
            },
            onDeleteSession = { session ->
                viewModel.deleteSession(session.id)
            },
            onDeleteAllByDate = {
                viewModel.deleteSessionsByDate(selectedDate)
            },
            onDismiss = { showDetailDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 広告バナー
        TopBannerAd(showAd = !adRemoved)

        // 日付ナビゲーション（←日付→形式）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ← 前日ボタン
            IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = WorkoutColors.TextPrimary
                )
            }

            // 日付表示
            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary
            )

            // → 翌日ボタン
            IconButton(
                onClick = {
                    if (selectedDate < currentLogicalDate()) {
                        selectedDate = selectedDate.plusDays(1)
                    }
                },
                enabled = selectedDate < currentLogicalDate()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = if (selectedDate < currentLogicalDate())
                        WorkoutColors.TextPrimary
                    else
                        WorkoutColors.TextSecondary.copy(alpha = 0.3f)
                )
            }
        }

        // メインコンテンツ
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (sessionsForDate.isEmpty()) {
                // データなしメッセージ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_workout_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WorkoutColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 日付合計サマリ
                DaySummaryCard(
                    totalDurationSeconds = totalDurationSeconds,
                    totalCalories = totalCalories,
                    totalWeight = totalWeight,
                    weightUnit = weightUnit,
                    onClick = { showDetailDialog = true }
                )

                // カテゴリ別サマリ
                // 筋トレ
                sessionsByType[WorkoutType.STRENGTH]?.let { sessions ->
                    val categoryWeight = sessions.sumOf { session ->
                        sessionWeights[session.id] ?: 0.0
                    }
                    CategorySummaryCard(
                        title = stringResource(R.string.workout_strength),
                        gradientColors = listOf(
                            WorkoutColors.StrengthCardStart,
                            WorkoutColors.StrengthCardEnd
                        ),
                        sessions = sessions,
                        exerciseNames = exerciseNames,
                        sessionWeights = sessionWeights,
                        weightUnit = weightUnit,
                        categoryWeight = categoryWeight,
                        categoryDurationSeconds = sessions.sumOf { it.durationSeconds },
                        categoryCalories = sessions.sumOf { it.caloriesBurned },
                        onClick = { showDetailDialog = true },
                        onEdit = { showDetailDialog = true },
                        onDelete = {
                            categoryToDelete = WorkoutType.STRENGTH
                            showDeleteCategoryDialog = true
                        }
                    )
                }

                // 有酸素
                sessionsByType[WorkoutType.CARDIO]?.let { sessions ->
                    CategorySummaryCard(
                        title = stringResource(R.string.workout_cardio),
                        gradientColors = listOf(
                            WorkoutColors.CardioCardStart,
                            WorkoutColors.CardioCardEnd
                        ),
                        sessions = sessions,
                        exerciseNames = exerciseNames,
                        sessionWeights = sessionWeights,
                        weightUnit = weightUnit,
                        categoryWeight = 0.0,
                        categoryDurationSeconds = sessions.sumOf { it.durationSeconds },
                        categoryCalories = sessions.sumOf { it.caloriesBurned },
                        onClick = { showDetailDialog = true },
                        onEdit = { showDetailDialog = true },
                        onDelete = {
                            categoryToDelete = WorkoutType.CARDIO
                            showDeleteCategoryDialog = true
                        }
                    )
                }

                // インターバル
                sessionsByType[WorkoutType.INTERVAL]?.let { sessions ->
                    CategorySummaryCard(
                        title = stringResource(R.string.workout_interval),
                        gradientColors = listOf(
                            WorkoutColors.IntervalCardStart,
                            WorkoutColors.IntervalCardEnd
                        ),
                        sessions = sessions,
                        exerciseNames = exerciseNames,
                        sessionWeights = sessionWeights,
                        weightUnit = weightUnit,
                        categoryWeight = 0.0,
                        categoryDurationSeconds = sessions.sumOf { it.durationSeconds },
                        categoryCalories = sessions.sumOf { it.caloriesBurned },
                        onClick = { showDetailDialog = true },
                        onEdit = { showDetailDialog = true },
                        onDelete = {
                            categoryToDelete = WorkoutType.INTERVAL
                            showDeleteCategoryDialog = true
                        }
                    )
                }

                // スタジオ
                sessionsByType[WorkoutType.STUDIO]?.let { sessions ->
                    CategorySummaryCard(
                        title = stringResource(R.string.workout_studio),
                        gradientColors = listOf(
                            WorkoutColors.StudioCardStart,
                            WorkoutColors.StudioCardEnd
                        ),
                        sessions = sessions,
                        exerciseNames = exerciseNames,
                        sessionWeights = sessionWeights,
                        weightUnit = weightUnit,
                        categoryWeight = 0.0,
                        categoryDurationSeconds = sessions.sumOf { it.durationSeconds },
                        categoryCalories = sessions.sumOf { it.caloriesBurned },
                        onClick = { showDetailDialog = true },
                        onEdit = { showDetailDialog = true },
                        onDelete = {
                            categoryToDelete = WorkoutType.STUDIO
                            showDeleteCategoryDialog = true
                        }
                    )
                }

                // その他
                sessionsByType[WorkoutType.OTHER]?.let { sessions ->
                    CategorySummaryCard(
                        title = stringResource(R.string.workout_other),
                        gradientColors = listOf(
                            WorkoutColors.OtherCardStart,
                            WorkoutColors.OtherCardEnd
                        ),
                        sessions = sessions,
                        exerciseNames = exerciseNames,
                        sessionWeights = sessionWeights,
                        weightUnit = weightUnit,
                        categoryWeight = 0.0,
                        categoryDurationSeconds = sessions.sumOf { it.durationSeconds },
                        categoryCalories = sessions.sumOf { it.caloriesBurned },
                        onClick = { showDetailDialog = true },
                        onEdit = { showDetailDialog = true },
                        onDelete = {
                            categoryToDelete = WorkoutType.OTHER
                            showDeleteCategoryDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 日付合計サマリカード
 */
@Composable
private fun DaySummaryCard(
    totalDurationSeconds: Int,
    totalCalories: Int,
    totalWeight: Double,
    weightUnit: WeightUnit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.GrandTotalBackground)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.day_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = stringResource(R.string.total_duration_label),
                    value = formatHms(totalDurationSeconds)
                )

                SummaryItem(
                    label = stringResource(R.string.total_calories_label),
                    value = "$totalCalories kcal"
                )

                SummaryItem(
                    label = stringResource(R.string.total_weight_label),
                    value = formatWeight(totalWeight, weightUnit)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WorkoutColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.GrandTotalText
        )
    }
}

/**
 * カテゴリ別サマリカード（編集・削除アイコン付き）
 */
@Composable
private fun CategorySummaryCard(
    title: String,
    gradientColors: List<Color>,
    sessions: List<WorkoutSessionEntity>,
    exerciseNames: Map<Long, String>,
    sessionWeights: Map<Long, Double>,
    weightUnit: WeightUnit,
    categoryWeight: Double,
    categoryDurationSeconds: Int,
    categoryCalories: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val gradient = Brush.horizontalGradient(colors = gradientColors)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            // タイトル行（カテゴリ名 + 編集・削除アイコン）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )

                // 編集・削除アイコン
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = WorkoutColors.PureBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = WorkoutColors.PureRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // カテゴリサマリ（重量、時間、カロリー）- 短い形式で表示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (categoryWeight > 0) {
                    Text(
                        text = formatWeight(categoryWeight, weightUnit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorkoutColors.TextPrimary
                    )
                }
                if (categoryDurationSeconds > 0) {
                    Text(
                        text = formatHms(categoryDurationSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorkoutColors.TextPrimary
                    )
                }
                if (categoryCalories > 0) {
                    Text(
                        text = "$categoryCalories kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorkoutColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 種目一覧（種目名と重量を表示）
            sessions.forEach { session ->
                val exerciseName = exerciseNames[session.exerciseId] ?: "Exercise #${session.exerciseId}"
                val sessionWeight = sessionWeights[session.id] ?: 0.0

                Text(
                    text = "• $exerciseName" + if (sessionWeight > 0) " (${formatWeight(sessionWeight, weightUnit)})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            }
        }
    }
}