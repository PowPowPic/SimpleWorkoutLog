package com.poweder.simpleworkoutlog.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.dao.DailyMaxWeight
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.DistanceUnit
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

// ========== グラデーション定義（統一感のため集約） ==========

object GraphGradients {
    // カロリーグラフ背景（強いグラデーション）
    val caloriesChartBackground = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2A2A2A),  // ダークグレー
            Color(0xFF4A4A4A),  // ミディアムグレー
            Color(0xFF6A6A6A)   // ライトグレー
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // 入口カード（アッシュグリーン強調）
    val entryCard = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF5A6B52),  // 濃いアッシュグリーン
            Color(0xFF8FA085),  // ミディアムアッシュグリーン
            Color(0xFFB0C2A7),  // ベースアッシュグリーン
            Color(0xFFD4E4CC)   // 明るいアッシュグリーン
        )
    )

    // 筋トレカード（グレー系グラデーション）
    val strengthCard = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF4A4A4A),  // 濃いグレー
            Color(0xFF6A6A6A),  // ミディアムグレー
            Color(0xFF9A9A9A)   // 明るいグレー
        )
    )

    // 有酸素カード（茶色系グラデーション）
    val cardioCard = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF4A3228),  // 濃いブラウン
            Color(0xFF6D4C41),  // ミディアムブラウン
            Color(0xFFA1887F)   // 明るいブラウン
        )
    )

    // スタジオカード（紫系グラデーション）
    val studioCard = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF3D3654),  // 濃い紫グレー
            Color(0xFF5E5370),  // ミディアム紫
            Color(0xFF9182AB)   // 明るいラベンダー
        )
    )

    // グラフダイアログ背景（筋トレ）
    val strengthDialogBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A2A2A),
            Color(0xFF3A3A3A),
            Color(0xFF4A4A4A)
        )
    )

    // グラフダイアログ背景（有酸素）
    val cardioDialogBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2A2520),
            Color(0xFF3A3028),
            Color(0xFF4A3830)
        )
    )

    // グラフダイアログ背景（スタジオ）
    val studioDialogBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF252030),
            Color(0xFF302A3A),
            Color(0xFF3A3444)
        )
    )

    // グラフチャート背景（共通）
    val chartBackground = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3A3A3A),
            Color(0xFF4A4A4A),
            Color(0xFF5A5A5A)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

/**
 * グラフ期間
 */
enum class GraphPeriod(val months: Int?, val labelKey: String) {
    ONE_MONTH(1, "1M"),
    THREE_MONTHS(3, "3M"),
    ONE_YEAR(12, "1Y"),
    ALL(null, "ALL")
}

/**
 * グラフ種類
 */
enum class GraphType {
    STRENGTH,   // 筋トレ（最大kg推移）
    CARDIO,     // 有酸素（累積距離）
    STUDIO      // スタジオ（累積回数）
}

/**
 * リセット対象
 */
sealed class ResetTarget {
    data class Strength(val exerciseId: Long) : ResetTarget()
    data class Cardio(val exerciseId: Long) : ResetTarget()
    data object Studio : ResetTarget()
}

/**
 * グラフ画面
 */
@Composable
fun GraphScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val adRemoved by viewModel.adRemoved.collectAsState()
    val graphResetDate by viewModel.graphResetDate.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val context = LocalContext.current

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

    // 期間選択
    var selectedPeriod by remember { mutableStateOf(GraphPeriod.ONE_MONTH) }

    // リセット確認ダイアログ（上段カロリーグラフ用）
    var showResetDialog by remember { mutableStateOf(false) }

    // グラフ選択ダイアログ
    var showGraphPickerDialog by remember { mutableStateOf(false) }

    // Strength用状態
    var showStrengthExercisePicker by remember { mutableStateOf(false) }
    var selectedStrengthExerciseId by remember { mutableStateOf<Long?>(null) }
    var selectedStrengthExerciseName by remember { mutableStateOf("") }
    var showStrengthGraphDialog by remember { mutableStateOf(false) }

    // Cardio用状態
    var showCardioExercisePicker by remember { mutableStateOf(false) }
    var selectedCardioExerciseId by remember { mutableStateOf<Long?>(null) }
    var selectedCardioExerciseName by remember { mutableStateOf("") }
    var showCardioGraphDialog by remember { mutableStateOf(false) }

    // Studio用状態
    var showStudioGraphDialog by remember { mutableStateOf(false) }

    // 下段グラフ用リセット確認
    var showGraphResetConfirmDialog by remember { mutableStateOf(false) }
    var resetTarget by remember { mutableStateOf<ResetTarget?>(null) }

    // 種目一覧
    val strengthExercises by viewModel.strengthExercises.collectAsState()
    val cardioExercises by viewModel.cardioExercises.collectAsState()

    // 最古のセッション日付（ALL用）
    val oldestDate by viewModel.getOldestSessionDate().collectAsState(initial = null)

    // 期間の開始日を計算
    val periodStartDate = remember(selectedPeriod, logicalDate, graphResetDate, oldestDate) {
        val baseStart = when (selectedPeriod) {
            GraphPeriod.ONE_MONTH -> logicalDate.minusMonths(1).plusDays(1)
            GraphPeriod.THREE_MONTHS -> logicalDate.minusMonths(3).plusDays(1)
            GraphPeriod.ONE_YEAR -> logicalDate.minusYears(1).plusDays(1)
            GraphPeriod.ALL -> oldestDate?.let { LocalDate.ofEpochDay(it) } ?: logicalDate.minusMonths(1)
        }
        val resetStart = graphResetDate?.let { LocalDate.ofEpochDay(it) }
        if (resetStart != null && resetStart.isAfter(baseStart)) resetStart else baseStart
    }

    // セッションデータを取得
    val sessions by viewModel.getSessionsBetweenDates(periodStartDate, logicalDate)
        .collectAsState(initial = emptyList())

    // 累積カロリー配列
    val cumulativeData = remember(sessions, periodStartDate, logicalDate, selectedPeriod) {
        calculateCumulativeCalories(
            sessions = sessions,
            startDate = periodStartDate,
            endDate = logicalDate,
            forceZeroStart = (selectedPeriod == GraphPeriod.ALL)
        )
    }

    // ===== ダイアログ群 =====

    // 上段カロリーグラフリセット確認
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.graph_reset_title)) },
            text = { Text(stringResource(R.string.graph_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetGraph()
                    showResetDialog = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // グラフ選択ダイアログ
    if (showGraphPickerDialog) {
        GraphPickerDialog(
            onDismiss = { showGraphPickerDialog = false },
            onSelect = { type ->
                showGraphPickerDialog = false
                when (type) {
                    GraphType.STRENGTH -> showStrengthExercisePicker = true
                    GraphType.CARDIO -> showCardioExercisePicker = true
                    GraphType.STUDIO -> showStudioGraphDialog = true
                }
            }
        )
    }

    // Strength種目選択
    if (showStrengthExercisePicker) {
        ExercisePickerDialog(
            exercises = strengthExercises,
            graphType = GraphType.STRENGTH,
            onDismiss = { showStrengthExercisePicker = false },
            onSelect = { exercise ->
                selectedStrengthExerciseId = exercise.id
                selectedStrengthExerciseName = exercise.getDisplayName(context)
                showStrengthExercisePicker = false
                showStrengthGraphDialog = true
            }
        )
    }

    // Strengthグラフダイアログ
    if (showStrengthGraphDialog && selectedStrengthExerciseId != null) {
        StrengthGraphDialog(
            exerciseId = selectedStrengthExerciseId!!,
            exerciseName = selectedStrengthExerciseName,
            viewModel = viewModel,
            weightUnit = weightUnit,
            onReset = {
                resetTarget = ResetTarget.Strength(selectedStrengthExerciseId!!)
                showGraphResetConfirmDialog = true
            },
            onDismiss = { showStrengthGraphDialog = false }
        )
    }

    // Cardio種目選択
    if (showCardioExercisePicker) {
        ExercisePickerDialog(
            exercises = cardioExercises,
            graphType = GraphType.CARDIO,
            onDismiss = { showCardioExercisePicker = false },
            onSelect = { exercise ->
                selectedCardioExerciseId = exercise.id
                selectedCardioExerciseName = exercise.getDisplayName(context)
                showCardioExercisePicker = false
                showCardioGraphDialog = true
            }
        )
    }

    // Cardioグラフダイアログ
    if (showCardioGraphDialog && selectedCardioExerciseId != null) {
        CardioGraphDialog(
            exerciseId = selectedCardioExerciseId!!,
            exerciseName = selectedCardioExerciseName,
            viewModel = viewModel,
            distanceUnit = distanceUnit,
            onReset = {
                resetTarget = ResetTarget.Cardio(selectedCardioExerciseId!!)
                showGraphResetConfirmDialog = true
            },
            onDismiss = { showCardioGraphDialog = false }
        )
    }

    // Studioグラフダイアログ
    if (showStudioGraphDialog) {
        StudioGraphDialog(
            viewModel = viewModel,
            onReset = {
                resetTarget = ResetTarget.Studio
                showGraphResetConfirmDialog = true
            },
            onDismiss = { showStudioGraphDialog = false }
        )
    }

    // 下段グラフ用リセット確認ダイアログ
    if (showGraphResetConfirmDialog && resetTarget != null) {
        AlertDialog(
            onDismissRequest = { showGraphResetConfirmDialog = false },
            title = { Text(stringResource(R.string.graph_reset_title)) },
            text = { Text(stringResource(R.string.graph_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    when (val target = resetTarget) {
                        is ResetTarget.Strength -> viewModel.resetStrengthGraph(target.exerciseId)
                        is ResetTarget.Cardio -> viewModel.resetCardioGraph(target.exerciseId)
                        is ResetTarget.Studio -> viewModel.resetStudioGraph()
                        null -> {}
                    }
                    showGraphResetConfirmDialog = false
                    resetTarget = null
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGraphResetConfirmDialog = false
                    resetTarget = null
                }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    // ===== UI本体 =====
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        TopBannerAd(showAd = !adRemoved)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 期間切替 + 現在ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GraphPeriod.entries.forEach { period ->
                        PeriodChip(
                            label = period.labelKey,
                            isSelected = selectedPeriod == period,
                            onClick = { selectedPeriod = period }
                        )
                    }
                }
                TextButton(
                    onClick = { logicalDate = currentLogicalDate() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.now), style = MaterialTheme.typography.labelMedium, color = WorkoutColors.AccentOrange)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.cumulative_calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            CumulativeCaloriesChart(data = cumulativeData, modifier = Modifier.fillMaxWidth().height(250.dp))

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.graph_reset_link),
                style = MaterialTheme.typography.bodySmall,
                color = WorkoutColors.TextSecondary,
                modifier = Modifier.clickable { showResetDialog = true }.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 下段：グラフ選択入口カード（アッシュグリーングラデーション強化）
            GraphSelectionCard(onClick = { showGraphPickerDialog = true })

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ========== 共通コンポーネント ==========

@Composable
private fun PeriodChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) WorkoutColors.AccentOrange else WorkoutColors.BackgroundDark
    val textColor = if (isSelected) Color.White else WorkoutColors.TextSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textColor)
    }
}

data class CumulativeDataPoint(val date: LocalDate, val dailyCalories: Int, val cumulativeCalories: Int)

private fun calculateCumulativeCalories(
    sessions: List<com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity>,
    startDate: LocalDate, endDate: LocalDate, forceZeroStart: Boolean = false
): List<CumulativeDataPoint> {
    val dailyCaloriesMap = sessions.groupBy { LocalDate.ofEpochDay(it.logicalDate) }
        .mapValues { (_, s) -> s.sumOf { it.caloriesBurned } }
    val result = mutableListOf<CumulativeDataPoint>()
    var cumulative = 0
    var currentDate = startDate
    while (!currentDate.isAfter(endDate)) {
        val daily = dailyCaloriesMap[currentDate] ?: 0
        cumulative += daily
        result.add(CumulativeDataPoint(currentDate, daily, cumulative))
        currentDate = currentDate.plusDays(1)
    }
    if (forceZeroStart && result.isNotEmpty() && result.first().cumulativeCalories > 0) {
        return listOf(CumulativeDataPoint(startDate.minusDays(1), 0, 0)) + result
    }
    return result
}

@Composable
private fun CumulativeCaloriesChart(data: List<CumulativeDataPoint>, modifier: Modifier = Modifier) {
    val maxCumulative = data.maxOfOrNull { it.cumulativeCalories } ?: 0
    val milestoneStep = calculateMilestoneStep(maxCumulative)
    val yAxisMax = if (maxCumulative == 0) 1000 else ((maxCumulative / milestoneStep) + 1) * milestoneStep
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d", Locale.getDefault()) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val xLabelIndices = remember(data.size) {
        if (data.size <= 8) data.indices.toList() else {
            val step = data.size / 7
            (0 until data.size step step).toList().take(8)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GraphGradients.caloriesChartBackground)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        if (data.isEmpty() || maxCumulative == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data), style = MaterialTheme.typography.bodyMedium, color = WorkoutColors.TextSecondary)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pL = 60.dp.toPx(); val pR = 16.dp.toPx(); val pT = 20.dp.toPx(); val pB = 40.dp.toPx()
                val cW = size.width - pL - pR; val cH = size.height - pT - pB
                drawMilestoneLines(milestoneStep, yAxisMax, pL, pT, cW, cH, numberFormat)
                if (data.size > 1) {
                    val path = Path()
                    data.forEachIndexed { i, p ->
                        val x = pL + (i.toFloat() / (data.size - 1)) * cW
                        val y = pT + cH - (p.cumulativeCalories.toFloat() / yAxisMax) * cH
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, WorkoutColors.AccentOrange, style = Stroke(width = 3.dp.toPx()))
                }
                if (data.isNotEmpty()) {
                    val last = data.last()
                    val lX = pL + cW; val lY = pT + cH - (last.cumulativeCalories.toFloat() / yAxisMax) * cH
                    drawCircle(WorkoutColors.AccentOrange, 8.dp.toPx(), Offset(lX, lY))
                    val label = "${numberFormat.format(last.cumulativeCalories)} kcal"
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FF6B35")
                            textSize = 14.sp.toPx(); isFakeBoldText = true
                            textAlign = if (lX + 80.dp.toPx() > size.width) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
                        }
                        drawText(label, if (lX + 80.dp.toPx() > size.width) lX - 12.dp.toPx() else lX + 12.dp.toPx(), lY - 12.dp.toPx(), paint)
                    }
                }
                drawXAxisLabels(data, xLabelIndices, dateFormatter, pL, pT, cW, cH)
            }
        }
    }
}

private fun calculateMilestoneStep(maxValue: Int): Int = when {
    maxValue < 10_000 -> 1_000; maxValue < 50_000 -> 5_000; maxValue < 200_000 -> 10_000; else -> 50_000
}

private fun DrawScope.drawMilestoneLines(step: Int, yMax: Int, pL: Float, pT: Float, cW: Float, cH: Float, nf: NumberFormat) {
    val mC = Color.White.copy(alpha = 0.2f); val tC = android.graphics.Color.argb(200, 200, 200, 200)
    var m = 0
    while (m <= yMax) {
        val y = pT + cH - (m.toFloat() / yMax) * cH
        drawLine(mC, Offset(pL, y), Offset(pL + cW, y), 1.dp.toPx())
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply { color = tC; textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT }
            drawText(if (m >= 1000) "${m / 1000}k" else m.toString(), pL - 8.dp.toPx(), y + 4.dp.toPx(), p)
        }
        m += step
    }
}

private fun DrawScope.drawXAxisLabels(data: List<CumulativeDataPoint>, indices: List<Int>, df: DateTimeFormatter, pL: Float, pT: Float, cW: Float, cH: Float) {
    val tC = android.graphics.Color.argb(200, 200, 200, 200)
    indices.forEach { i ->
        if (i < data.size) {
            val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
            drawContext.canvas.nativeCanvas.apply {
                val p = android.graphics.Paint().apply { color = tC; textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                drawText(data[i].date.format(df), x, pT + cH + 20.dp.toPx(), p)
            }
        }
    }
}

// ========== 入口カード（アッシュグリーングラデーション強化） ==========

@Composable
private fun GraphSelectionCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush = GraphGradients.entryCard)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(
                stringResource(R.string.which_graph_to_see),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A2518) // より濃いダークグリーン文字
            )
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1A2518))
        }
    }
}

// ========== グラフ選択ダイアログ ==========

@Composable
private fun GraphPickerDialog(onDismiss: () -> Unit, onSelect: (GraphType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.graph_picker_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 筋トレカード（グレー系グラデーション）
                GradientPickerItem(
                    label = stringResource(R.string.graph_picker_strength),
                    gradient = GraphGradients.strengthCard,
                    textColor = Color.White
                ) { onSelect(GraphType.STRENGTH) }

                // 有酸素カード（茶色系グラデーション）
                GradientPickerItem(
                    label = stringResource(R.string.graph_picker_cardio),
                    gradient = GraphGradients.cardioCard,
                    textColor = Color.White
                ) { onSelect(GraphType.CARDIO) }

                // スタジオカード（紫系グラデーション）
                GradientPickerItem(
                    label = stringResource(R.string.graph_picker_studio),
                    gradient = GraphGradients.studioCard,
                    textColor = Color.White
                ) { onSelect(GraphType.STUDIO) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
private fun GradientPickerItem(
    label: String,
    gradient: Brush,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

// ========== 種目選択ダイアログ（共通・グラデーション対応） ==========

@Composable
private fun ExercisePickerDialog(
    exercises: List<ExerciseEntity>,
    graphType: GraphType,
    onDismiss: () -> Unit,
    onSelect: (ExerciseEntity) -> Unit
) {
    val context = LocalContext.current

    // グラフタイプに応じたグラデーション
    val itemGradient = when (graphType) {
        GraphType.STRENGTH -> GraphGradients.strengthCard
        GraphType.CARDIO -> GraphGradients.cardioCard
        GraphType.STUDIO -> GraphGradients.studioCard
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_exercise), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (exercises.isEmpty()) {
                    Text(stringResource(R.string.no_data), style = MaterialTheme.typography.bodyMedium, color = WorkoutColors.TextSecondary, modifier = Modifier.padding(16.dp))
                } else {
                    exercises.forEach { ex ->
                        GradientPickerItem(
                            label = ex.getDisplayName(context),
                            gradient = itemGradient,
                            textColor = Color.White
                        ) { onSelect(ex) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

// ========== Strengthグラフダイアログ ==========

@Composable
private fun StrengthGraphDialog(
    exerciseId: Long, exerciseName: String, viewModel: WorkoutViewModel,
    weightUnit: WeightUnit, onReset: () -> Unit, onDismiss: () -> Unit
) {
    val resetDate by viewModel.getStrengthGraphResetDate(exerciseId).collectAsState(initial = null)
    val startDate = resetDate ?: 0L
    val data by viewModel.getDailyMaxWeightForExercise(exerciseId, startDate).collectAsState(initial = emptyList())

    GraphDialogContainer(
        title = exerciseName,
        backgroundGradient = GraphGradients.strengthDialogBackground,
        onDismiss = onDismiss,
        onReset = onReset
    ) {
        MaxWeightChart(data = data, weightUnit = weightUnit, modifier = Modifier.fillMaxWidth().height(280.dp))
    }
}

@Composable
private fun MaxWeightChart(data: List<DailyMaxWeight>, weightUnit: WeightUnit, modifier: Modifier = Modifier) {
    val maxW = data.maxOfOrNull { it.maxWeight } ?: 0.0
    val minW = data.minOfOrNull { it.maxWeight } ?: 0.0
    val yRange = if (maxW == minW) 10.0 else (maxW - minW)
    val yMin = (minW - yRange * 0.1).coerceAtLeast(0.0)
    val yMax = maxW + yRange * 0.1
    val df = remember { DateTimeFormatter.ofPattern("M/d", Locale.getDefault()) }
    val xIdx = remember(data.size) { if (data.size <= 6) data.indices.toList() else (0 until data.size step (data.size / 5)).toList().take(6) }

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(GraphGradients.chartBackground).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        if (data.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_data), color = WorkoutColors.TextSecondary) }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val pL = 50.dp.toPx(); val pR = 16.dp.toPx(); val pT = 20.dp.toPx(); val pB = 40.dp.toPx()
                val cW = size.width - pL - pR; val cH = size.height - pT - pB
                // Y軸
                val yStep = (yMax - yMin) / 4
                for (i in 0..4) {
                    val v = yMin + yStep * i
                    val y = pT + cH - ((v - yMin) / (yMax - yMin)).toFloat() * cH
                    drawLine(Color.White.copy(alpha = 0.2f), Offset(pL, y), Offset(pL + cW, y), 1.dp.toPx())
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 200, 200, 200); textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT }
                        drawText("${v.toInt()}", pL - 8.dp.toPx(), y + 4.dp.toPx(), p)
                    }
                }
                // 折れ線
                if (data.size > 1) {
                    val path = Path()
                    data.forEachIndexed { i, pt ->
                        val x = pL + (i.toFloat() / (data.size - 1)) * cW
                        val y = pT + cH - ((pt.maxWeight - yMin) / (yMax - yMin)).toFloat() * cH
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, WorkoutColors.AccentOrange, style = Stroke(width = 3.dp.toPx()))
                }
                // 点
                data.forEachIndexed { i, pt ->
                    val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
                    val y = pT + cH - ((pt.maxWeight - yMin) / (yMax - yMin)).toFloat() * cH
                    drawCircle(WorkoutColors.AccentOrange, 5.dp.toPx(), Offset(x, y))
                }
                // 最新点ラベル
                if (data.isNotEmpty()) {
                    val last = data.last()
                    val lX = pL + cW; val lY = pT + cH - ((last.maxWeight - yMin) / (yMax - yMin)).toFloat() * cH
                    drawCircle(WorkoutColors.AccentOrange, 8.dp.toPx(), Offset(lX, lY))
                    val label = when (weightUnit) { WeightUnit.KG -> "${last.maxWeight.toInt()} kg"; WeightUnit.LB -> "${(last.maxWeight * 2.20462).toInt()} lbs" }
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FF6B35"); textSize = 14.sp.toPx(); isFakeBoldText = true; textAlign = android.graphics.Paint.Align.RIGHT }
                        drawText(label, lX - 12.dp.toPx(), lY - 12.dp.toPx(), p)
                    }
                }
                // X軸
                xIdx.forEach { i ->
                    if (i < data.size) {
                        val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
                        val d = LocalDate.ofEpochDay(data[i].date)
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 200, 200, 200); textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                            drawText(d.format(df), x, pT + cH + 20.dp.toPx(), p)
                        }
                    }
                }
            }
        }
    }
}

// ========== Cardioグラフダイアログ ==========

@Composable
private fun CardioGraphDialog(
    exerciseId: Long, exerciseName: String, viewModel: WorkoutViewModel,
    distanceUnit: DistanceUnit, onReset: () -> Unit, onDismiss: () -> Unit
) {
    val resetDate by viewModel.getCardioGraphResetDate(exerciseId).collectAsState(initial = null)
    val startDate = resetDate ?: 0L
    val dailyData by viewModel.getDailyDistanceForCardioExercise(exerciseId, startDate).collectAsState(initial = emptyList())

    // 累積に変換
    val cumulativeData = remember(dailyData) {
        var running = 0.0
        dailyData.sortedBy { it.date }.map { d ->
            running += d.totalDistance
            d.date to running
        }
    }

    GraphDialogContainer(
        title = exerciseName,
        backgroundGradient = GraphGradients.cardioDialogBackground,
        onDismiss = onDismiss,
        onReset = onReset
    ) {
        CumulativeDistanceChart(data = cumulativeData, distanceUnit = distanceUnit, modifier = Modifier.fillMaxWidth().height(280.dp))
    }
}

@Composable
private fun CumulativeDistanceChart(data: List<Pair<Long, Double>>, distanceUnit: DistanceUnit, modifier: Modifier = Modifier) {
    val maxD = data.maxOfOrNull { it.second } ?: 0.0
    val yMax = if (maxD == 0.0) 10.0 else maxD * 1.1
    val df = remember { DateTimeFormatter.ofPattern("M/d", Locale.getDefault()) }
    val xIdx = remember(data.size) { if (data.size <= 6) data.indices.toList() else (0 until data.size step (data.size / 5)).toList().take(6) }

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(GraphGradients.chartBackground).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        if (data.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_data), color = WorkoutColors.TextSecondary) }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val pL = 50.dp.toPx(); val pR = 16.dp.toPx(); val pT = 20.dp.toPx(); val pB = 40.dp.toPx()
                val cW = size.width - pL - pR; val cH = size.height - pT - pB
                // Y軸
                val yStep = yMax / 4
                for (i in 0..4) {
                    val v = yStep * i
                    val y = pT + cH - (v / yMax).toFloat() * cH
                    drawLine(Color.White.copy(alpha = 0.2f), Offset(pL, y), Offset(pL + cW, y), 1.dp.toPx())
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 200, 200, 200); textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT }
                        val displayVal = when (distanceUnit) { DistanceUnit.KM -> v; DistanceUnit.MILE -> v * 0.621371 }
                        drawText("${displayVal.toInt()}", pL - 8.dp.toPx(), y + 4.dp.toPx(), p)
                    }
                }
                // 折れ線
                if (data.size > 1) {
                    val path = Path()
                    data.forEachIndexed { i, (_, cum) ->
                        val x = pL + (i.toFloat() / (data.size - 1)) * cW
                        val y = pT + cH - (cum / yMax).toFloat() * cH
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, WorkoutColors.AccentOrange, style = Stroke(width = 3.dp.toPx()))
                }
                // 点
                data.forEachIndexed { i, (_, cum) ->
                    val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
                    val y = pT + cH - (cum / yMax).toFloat() * cH
                    drawCircle(WorkoutColors.AccentOrange, 5.dp.toPx(), Offset(x, y))
                }
                // 最新点ラベル
                if (data.isNotEmpty()) {
                    val (_, lastCum) = data.last()
                    val lX = pL + cW; val lY = pT + cH - (lastCum / yMax).toFloat() * cH
                    drawCircle(WorkoutColors.AccentOrange, 8.dp.toPx(), Offset(lX, lY))
                    val label = when (distanceUnit) { DistanceUnit.KM -> "${String.format("%.1f", lastCum)} km"; DistanceUnit.MILE -> "${String.format("%.1f", lastCum * 0.621371)} mi" }
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FF6B35"); textSize = 14.sp.toPx(); isFakeBoldText = true; textAlign = android.graphics.Paint.Align.RIGHT }
                        drawText(label, lX - 12.dp.toPx(), lY - 12.dp.toPx(), p)
                    }
                }
                // X軸
                xIdx.forEach { i ->
                    if (i < data.size) {
                        val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
                        val d = LocalDate.ofEpochDay(data[i].first)
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 200, 200, 200); textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                            drawText(d.format(df), x, pT + cH + 20.dp.toPx(), p)
                        }
                    }
                }
            }
        }
    }
}

// ========== Studioグラフダイアログ ==========

@Composable
private fun StudioGraphDialog(viewModel: WorkoutViewModel, onReset: () -> Unit, onDismiss: () -> Unit) {
    val resetDate by viewModel.getStudioGraphResetDate().collectAsState(initial = null)
    val startDate = resetDate ?: 0L
    val dailyData by viewModel.getDailyStudioSessionCount(startDate).collectAsState(initial = emptyList())

    // 累積に変換
    val cumulativeData = remember(dailyData) {
        var running = 0
        dailyData.sortedBy { it.date }.map { d ->
            running += d.sessionCount
            d.date to running
        }
    }

    GraphDialogContainer(
        title = stringResource(R.string.graph_picker_studio),
        backgroundGradient = GraphGradients.studioDialogBackground,
        onDismiss = onDismiss,
        onReset = onReset
    ) {
        CumulativeSessionCountChart(data = cumulativeData, modifier = Modifier.fillMaxWidth().height(280.dp))
    }
}

@Composable
private fun CumulativeSessionCountChart(data: List<Pair<Long, Int>>, modifier: Modifier = Modifier) {
    val maxC = data.maxOfOrNull { it.second } ?: 0
    val yMax = if (maxC == 0) 10 else (maxC * 1.1).toInt()
    val df = remember { DateTimeFormatter.ofPattern("M/d", Locale.getDefault()) }
    val xIdx = remember(data.size) { if (data.size <= 6) data.indices.toList() else (0 until data.size step (data.size / 5)).toList().take(6) }

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(GraphGradients.chartBackground).border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        if (data.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_data), color = WorkoutColors.TextSecondary) }
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val pL = 50.dp.toPx(); val pR = 16.dp.toPx(); val pT = 20.dp.toPx(); val pB = 40.dp.toPx()
                val cW = size.width - pL - pR; val cH = size.height - pT - pB
                // Y軸
                val yStep = yMax / 4
                for (i in 0..4) {
                    val v = yStep * i
                    val y = pT + cH - (v.toFloat() / yMax) * cH
                    drawLine(Color.White.copy(alpha = 0.2f), Offset(pL, y), Offset(pL + cW, y), 1.dp.toPx())
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 200, 200, 200); textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT }
                        drawText("$v", pL - 8.dp.toPx(), y + 4.dp.toPx(), p)
                    }
                }
                // 折れ線
                if (data.size > 1) {
                    val path = Path()
                    data.forEachIndexed { i, (_, cum) ->
                        val x = pL + (i.toFloat() / (data.size - 1)) * cW
                        val y = pT + cH - (cum.toFloat() / yMax) * cH
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, WorkoutColors.AccentOrange, style = Stroke(width = 3.dp.toPx()))
                }
                // 点
                data.forEachIndexed { i, (_, cum) ->
                    val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
                    val y = pT + cH - (cum.toFloat() / yMax) * cH
                    drawCircle(WorkoutColors.AccentOrange, 5.dp.toPx(), Offset(x, y))
                }
                // 最新点ラベル
                if (data.isNotEmpty()) {
                    val (_, lastCum) = data.last()
                    val lX = pL + cW; val lY = pT + cH - (lastCum.toFloat() / yMax) * cH
                    drawCircle(WorkoutColors.AccentOrange, 8.dp.toPx(), Offset(lX, lY))
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#FF6B35"); textSize = 14.sp.toPx(); isFakeBoldText = true; textAlign = android.graphics.Paint.Align.RIGHT }
                        drawText("$lastCum", lX - 12.dp.toPx(), lY - 12.dp.toPx(), p)
                    }
                }
                // X軸
                xIdx.forEach { i ->
                    if (i < data.size) {
                        val x = pL + (i.toFloat() / max(1, data.size - 1)) * cW
                        val d = LocalDate.ofEpochDay(data[i].first)
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply { color = android.graphics.Color.argb(200, 200, 200, 200); textSize = 9.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                            drawText(d.format(df), x, pT + cH + 20.dp.toPx(), p)
                        }
                    }
                }
            }
        }
    }
}

// ========== グラフダイアログ共通コンテナ（グラデーション対応） ==========

@Composable
private fun GraphDialogContainer(
    title: String,
    backgroundGradient: Brush,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundGradient)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = WorkoutColors.TextPrimary)
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = WorkoutColors.AccentOrange) }
                }
                Spacer(Modifier.height(16.dp))
                content()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.graph_reset),
                    style = MaterialTheme.typography.bodySmall,
                    color = WorkoutColors.TextSecondary,
                    modifier = Modifier.clickable { onReset() }.padding(vertical = 4.dp)
                )
            }
        }
    }
}