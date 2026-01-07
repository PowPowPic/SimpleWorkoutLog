package com.poweder.simpleworkoutlog.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.util.DistanceUnit
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.formatWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 日付詳細ダイアログ（履歴画面・カレンダー画面共通）
 * 種目ごとにカードを表示
 */
@Composable
fun WorkoutDayDetailDialog(
    date: LocalDate,
    sessions: List<WorkoutSessionEntity>,
    exerciseNames: Map<Long, String>,
    sessionWeights: Map<Long, Double>,
    weightUnit: WeightUnit,
    distanceUnit: DistanceUnit = DistanceUnit.KM,
    onEditSession: (WorkoutSessionEntity) -> Unit,
    onDeleteSession: (WorkoutSessionEntity) -> Unit,
    onDeleteAllByDate: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            .withLocale(Locale.getDefault())
    }

    // 合計計算
    val totalDuration = sessions.sumOf { it.durationMinutes }
    val totalCalories = sessions.sumOf { it.caloriesBurned }
    val totalWeight = sessionWeights.values.sum()

    // 全削除確認ダイアログ
    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text(stringResource(R.string.delete_all_day_title)) },
            text = { Text(stringResource(R.string.delete_all_day_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirm = false
                        onDeleteAllByDate()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.common_ok), color = WorkoutColors.PureRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(WorkoutColors.BackgroundDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ヘッダー（日付 + 削除アイコン）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(dateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutColors.TextPrimary
                    )

                    // 全削除ボタン
                    IconButton(onClick = { showDeleteAllConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete All",
                            tint = WorkoutColors.PureRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // スクロール可能なコンテンツ
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 日付合計サマリ
                    DaySummaryCard(
                        totalDuration = totalDuration,
                        totalCalories = totalCalories,
                        totalWeight = totalWeight,
                        weightUnit = weightUnit
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 種目ごとにカードを表示
                    sessions.forEach { session ->
                        // 種目名を取得（見つからない場合はexerciseIdを表示）
                        val exerciseName = exerciseNames[session.exerciseId] 
                            ?: "Exercise #${session.exerciseId}"
                        val sessionWeight = sessionWeights[session.id] ?: 0.0

                        ExerciseCard(
                            session = session,
                            exerciseName = exerciseName,
                            sessionWeight = sessionWeight,
                            weightUnit = weightUnit,
                            distanceUnit = distanceUnit,
                            onEdit = { onEditSession(session) },
                            onDelete = { onDeleteSession(session) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 閉じるボタン
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.common_close), color = WorkoutColors.TextSecondary)
                }
            }
        }
    }
}

/**
 * 日付合計サマリカード
 */
@Composable
private fun DaySummaryCard(
    totalDuration: Int,
    totalCalories: Int,
    totalWeight: Double,
    weightUnit: WeightUnit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.GrandTotalBackground)
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
                    value = formatDuration(totalDuration)
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
 * 種目ごとのカード（種目名をタイトルに表示）
 */
@Composable
private fun ExerciseCard(
    session: WorkoutSessionEntity,
    exerciseName: String,
    sessionWeight: Double,
    weightUnit: WeightUnit,
    distanceUnit: DistanceUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 削除確認ダイアログ
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_session_title)) },
            text = { Text(stringResource(R.string.delete_session_message, exerciseName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.common_ok), color = WorkoutColors.PureRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // カテゴリ別の色
    val gradientColors = when (session.workoutType) {
        WorkoutType.STRENGTH -> listOf(WorkoutColors.StrengthCardStart, WorkoutColors.StrengthCardEnd)
        WorkoutType.CARDIO -> listOf(WorkoutColors.CardioCardStart, WorkoutColors.CardioCardEnd)
        WorkoutType.INTERVAL -> listOf(WorkoutColors.IntervalCardStart, WorkoutColors.IntervalCardEnd)
        WorkoutType.STUDIO -> listOf(WorkoutColors.StudioCardStart, WorkoutColors.StudioCardEnd)
        WorkoutType.OTHER -> listOf(WorkoutColors.OtherCardStart, WorkoutColors.OtherCardEnd)
        else -> listOf(WorkoutColors.BackgroundMedium, WorkoutColors.BackgroundMedium)
    }

    val gradient = Brush.horizontalGradient(colors = gradientColors)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onEdit() }
            .padding(12.dp)
    ) {
        Column {
            // タイトル行（種目名 + 編集・削除アイコン）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 種目名を表示（太字で目立つように）
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.weight(1f)
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
                        onClick = { showDeleteConfirm = true },
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

            // 詳細情報（挙上重量、距離、運動時間、消費カロリー）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 挙上重量（筋トレの場合、または重量がある場合）
                if (sessionWeight > 0) {
                    DetailItem(
                        label = stringResource(R.string.total_weight_label),
                        value = formatWeight(sessionWeight, weightUnit)
                    )
                }

                // 距離（有酸素の場合）
                if (session.distance > 0) {
                    DetailItem(
                        label = stringResource(R.string.distance),
                        value = "%.2f %s".format(session.distance, distanceUnit.symbol)
                    )
                }

                // 運動時間
                if (session.durationMinutes > 0) {
                    DetailItem(
                        label = stringResource(R.string.total_duration_label),
                        value = formatDuration(session.durationMinutes)
                    )
                }

                // 消費カロリー
                if (session.caloriesBurned > 0) {
                    DetailItem(
                        label = stringResource(R.string.total_calories_label),
                        value = "${session.caloriesBurned} kcal"
                    )
                }
            }
            
            // 何も表示するものがない場合のフォールバック
            if (sessionWeight <= 0 && session.durationMinutes <= 0 && session.caloriesBurned <= 0) {
                Text(
                    text = "---",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.TextSecondary
                )
            }
        }
    }
}


@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WorkoutColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary
        )
    }
}

/**
 * 分数を時間:分形式にフォーマット
 */
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) {
        "${hours}h ${mins}m"
    } else {
        "${mins}m"
    }
}
