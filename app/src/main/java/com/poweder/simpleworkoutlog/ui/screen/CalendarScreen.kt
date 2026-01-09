package com.poweder.simpleworkoutlog.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.WorkoutDayDetailDialog
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import com.poweder.simpleworkoutlog.util.formatHms
import com.poweder.simpleworkoutlog.util.formatWeight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onNavigateToStrengthEdit: (Long) -> Unit = {},
    onNavigateToCardioEdit: (Long) -> Unit = {},
    onNavigateToStudioEdit: (Long) -> Unit = {},
    onNavigateToOtherEdit: (Long) -> Unit = {}
) {
    val weightUnit by viewModel.weightUnit.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val adRemoved by viewModel.adRemoved.collectAsState()

    // 選択中の月
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    // 選択した日付（詳細ダイアログ用）
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // 月のワークアウト日セット
    val workoutDates by viewModel.getWorkoutDatesForMonth(currentMonth).collectAsState(initial = emptySet())

    // 月の統計
    val monthlyStats by viewModel.getMonthlyStats(currentMonth).collectAsState(initial = MonthlyStats())

    // 選択日のセッション
    val sessionsForDate by selectedDate?.let { date ->
        viewModel.getSessionsForDate(date).collectAsState(initial = emptyList())
    } ?: remember { mutableStateOf(emptyList()) }

    // 選択日のセット（重量計算用）
    val setsForDate by selectedDate?.let { date ->
        viewModel.getSetsForDate(date).collectAsState(initial = emptyList())
    } ?: remember { mutableStateOf(emptyList()) }

    // 全種目リスト（直接取得）
    val allExercises by viewModel.allExercises.collectAsState()

    // 種目名マップを作成（allExercisesから直接）
    val exerciseNames = remember(allExercises) {
        allExercises.associate { exercise ->
            exercise.id to (exercise.customName ?: exercise.name)
        }
    }

    // セッションIDごとの総重量を計算
    val sessionWeights = remember(setsForDate) {
        setsForDate.groupBy { it.sessionId }
            .mapValues { (_, sets) -> sets.sumOf { it.weight * it.reps } }
    }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy年 M月", Locale.getDefault())
    }

    // 詳細ダイアログ
    selectedDate?.let { date ->
        if (sessionsForDate.isNotEmpty()) {
            WorkoutDayDetailDialog(
                date = date,
                sessions = sessionsForDate,
                exerciseNames = exerciseNames,
                sessionWeights = sessionWeights,
                weightUnit = weightUnit,
                distanceUnit = distanceUnit,
                onEditSession = { session ->
                    // workoutTypeに応じて編集画面へ遷移
                    selectedDate = null
                    when (session.workoutType) {
                        com.poweder.simpleworkoutlog.data.entity.WorkoutType.STRENGTH -> onNavigateToStrengthEdit(session.id)
                        com.poweder.simpleworkoutlog.data.entity.WorkoutType.CARDIO -> onNavigateToCardioEdit(session.id)
                        com.poweder.simpleworkoutlog.data.entity.WorkoutType.STUDIO -> onNavigateToStudioEdit(session.id)
                        com.poweder.simpleworkoutlog.data.entity.WorkoutType.OTHER -> onNavigateToOtherEdit(session.id)
                        else -> {} // INTERVAL等は今回対象外
                    }
                },
                onDeleteSession = { session ->
                    viewModel.deleteSession(session.id)
                },
                onDeleteAllByDate = {
                    viewModel.deleteSessionsByDate(date)
                },
                onDismiss = { selectedDate = null }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 広告バナー
        TopBannerAd(showAd = !adRemoved)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 月ナビゲーション（←年月→形式）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ← 前月ボタン
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = WorkoutColors.TextPrimary
                    )
                }

                // 年月表示
                Text(
                    text = currentMonth.atDay(1).format(monthFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )

                // → 翌月ボタン
                IconButton(
                    onClick = {
                        if (currentMonth < YearMonth.now()) {
                            currentMonth = currentMonth.plusMonths(1)
                        }
                    },
                    enabled = currentMonth < YearMonth.now()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = if (currentMonth < YearMonth.now())
                            WorkoutColors.TextPrimary
                        else
                            WorkoutColors.TextSecondary.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // カレンダーグリッド（月曜日始まり）
            CalendarGrid(
                currentMonth = currentMonth,
                workoutDates = workoutDates,
                onDateClick = { date ->
                    if (date in workoutDates) {
                        selectedDate = date
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 月集計
            MonthlySummaryCard(
                stats = monthlyStats,
                weightUnit = weightUnit
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * カレンダーグリッド（月曜日始まり、土日色なし）
 */
@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    workoutDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val today = currentLogicalDate()
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    // 月曜日始まり（月曜日=0, 火曜日=1, ... 日曜日=6）
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1) % 7

    // 曜日ヘッダー（月曜日始まり、色なし）
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val daysOfWeek = listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )
        daysOfWeek.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 日付グリッド
    var dayCounter = 1
    val totalWeeks = ((daysInMonth + firstDayOfWeek - 1) / 7) + 1

    for (week in 0 until totalWeeks) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (dayOfWeek in 0..6) {
                val dayIndex = week * 7 + dayOfWeek

                if (dayIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                    val date = currentMonth.atDay(dayCounter)
                    val isWorkoutDay = date in workoutDates
                    val isToday = date == today
                    val isFuture = date > today

                    DayCell(
                        day = dayCounter,
                        isWorkoutDay = isWorkoutDay,
                        isToday = isToday,
                        isFuture = isFuture,
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                    dayCounter++
                } else {
                    // 空セル
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * 日付セル（土日色なし）
 */
@Composable
private fun DayCell(
    day: Int,
    isWorkoutDay: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        isFuture -> WorkoutColors.TextSecondary.copy(alpha = 0.3f)
        isWorkoutDay -> WorkoutColors.PureRed
        else -> WorkoutColors.TextPrimary
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                if (isToday) WorkoutColors.AccentOrange.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable(enabled = isWorkoutDay && !isFuture) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isWorkoutDay) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * 月集計カード
 */
@Composable
private fun MonthlySummaryCard(
    stats: MonthlyStats,
    weightUnit: com.poweder.simpleworkoutlog.util.WeightUnit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.GrandTotalBackground)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.monthly_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ワークアウト日数
            SummaryRow(
                label = stringResource(R.string.workout_days),
                value = stringResource(R.string.days_format, stats.workoutDays)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 平均運動時間（h:mm:ss形式）
            SummaryRow(
                label = stringResource(R.string.average_duration),
                value = formatHms(stats.averageDurationSeconds)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 総運動時間（h:mm:ss形式）
            SummaryRow(
                label = stringResource(R.string.total_duration_month),
                value = formatHms(stats.totalDurationSeconds)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 総消費カロリー
            SummaryRow(
                label = stringResource(R.string.total_calories_month),
                value = "${stats.totalCalories} kcal"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 総挙上重量
            SummaryRow(
                label = stringResource(R.string.total_weight_month),
                value = formatWeight(stats.totalWeight, weightUnit)
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WorkoutColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.GrandTotalText
        )
    }
}

/**
 * 月間統計データクラス（秒単位）
 */
data class MonthlyStats(
    val workoutDays: Int = 0,
    val totalDurationSeconds: Int = 0,
    val averageDurationSeconds: Int = 0,
    val totalCalories: Int = 0,
    val totalWeight: Double = 0.0
)