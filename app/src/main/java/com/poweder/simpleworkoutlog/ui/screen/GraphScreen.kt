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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

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
 * グラフ種類（Phase0: 選択用enum）
 */
enum class GraphType {
    STRENGTH,   // 筋トレ（最大kg推移）
    CARDIO,     // 有酸素（累積距離）
    STUDIO      // スタジオ（累積回数）
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

    // リセット確認ダイアログ
    var showResetDialog by remember { mutableStateOf(false) }

    // グラフ選択ダイアログ（Phase0）
    var showGraphPickerDialog by remember { mutableStateOf(false) }
    var selectedGraphType by remember { mutableStateOf<GraphType?>(null) }
    var showComingSoonDialog by remember { mutableStateOf(false) }

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
        // グラフリセット日があれば、それより古い日付は使わない
        val resetStart = graphResetDate?.let { LocalDate.ofEpochDay(it) }
        if (resetStart != null && resetStart.isAfter(baseStart)) resetStart else baseStart
    }

    // セッションデータを取得
    val sessions by viewModel.getSessionsBetweenDates(periodStartDate, logicalDate)
        .collectAsState(initial = emptyList())

    // 日別カロリーを集計し、累積配列を生成（ALLのときは0開始点を追加）
    val cumulativeData = remember(sessions, periodStartDate, logicalDate, selectedPeriod) {
        calculateCumulativeCalories(
            sessions = sessions,
            startDate = periodStartDate,
            endDate = logicalDate,
            forceZeroStart = (selectedPeriod == GraphPeriod.ALL)
        )
    }

    // リセット確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.graph_reset_title)) },
            text = { Text(stringResource(R.string.graph_reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetGraph()
                        showResetDialog = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        )
    }

    // グラフ選択ダイアログ
    if (showGraphPickerDialog) {
        GraphPickerDialog(
            onDismiss = { showGraphPickerDialog = false },
            onSelect = { type ->
                selectedGraphType = type
                showGraphPickerDialog = false
                showComingSoonDialog = true
            }
        )
    }

    // 次フェーズ実装予定ダイアログ
    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            title = { Text(stringResource(R.string.coming_soon)) },
            text = { Text(stringResource(R.string.graph_coming_soon)) },
            confirmButton = {
                TextButton(onClick = { showComingSoonDialog = false }) {
                    Text(text = stringResource(id = R.string.common_close))
                }
            }
        )
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

            // ===== 上段：累積カロリーグラフ =====

            // 期間切替 + 現在ボタン + リセット
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 期間切替チップ
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GraphPeriod.entries.forEach { period ->
                        PeriodChip(
                            label = period.labelKey,
                            isSelected = selectedPeriod == period,
                            onClick = { selectedPeriod = period }
                        )
                    }
                }

                // 現在ボタン
                TextButton(
                    onClick = { logicalDate = currentLogicalDate() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.now),
                        style = MaterialTheme.typography.labelMedium,
                        color = WorkoutColors.AccentOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // グラフタイトル
            Text(
                text = stringResource(R.string.cumulative_calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // グラフ本体
            CumulativeCaloriesChart(
                data = cumulativeData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // リセットリンク
            Text(
                text = stringResource(R.string.graph_reset_link),
                style = MaterialTheme.typography.bodySmall,
                color = WorkoutColors.TextSecondary,
                modifier = Modifier
                    .clickable { showResetDialog = true }
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 下段：グラフ選択入口カード =====
            GraphSelectionCard(
                onClick = { showGraphPickerDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 期間選択チップ
 */
@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * 累積カロリーデータポイント
 */
data class CumulativeDataPoint(
    val date: LocalDate,
    val dailyCalories: Int,
    val cumulativeCalories: Int
)

/**
 * セッションから日別カロリーを集計し、累積配列を生成
 * @param forceZeroStart ALLモードのとき、開始点を0から始める
 */
private fun calculateCumulativeCalories(
    sessions: List<com.poweder.simpleworkoutlog.data.entity.WorkoutSessionEntity>,
    startDate: LocalDate,
    endDate: LocalDate,
    forceZeroStart: Boolean = false
): List<CumulativeDataPoint> {
    // 日別カロリーをマップ化
    val dailyCaloriesMap = sessions
        .groupBy { LocalDate.ofEpochDay(it.logicalDate) }
        .mapValues { (_, daySessions) -> daySessions.sumOf { it.caloriesBurned } }

    // 期間内の全日付で累積を計算
    val result = mutableListOf<CumulativeDataPoint>()
    var cumulative = 0
    var currentDate = startDate

    while (!currentDate.isAfter(endDate)) {
        val daily = dailyCaloriesMap[currentDate] ?: 0
        cumulative += daily
        result.add(CumulativeDataPoint(currentDate, daily, cumulative))
        currentDate = currentDate.plusDays(1)
    }

    // ALLモードかつデータがある場合、先頭に0開始点を明示的に追加
    // （すでに開始日が0ならそのまま、そうでなければ0点を挿入）
    if (forceZeroStart && result.isNotEmpty() && result.first().cumulativeCalories > 0) {
        // 開始日の前日に0点を挿入（視覚的に0から立ち上がるように）
        val zeroPoint = CumulativeDataPoint(
            date = startDate.minusDays(1),
            dailyCalories = 0,
            cumulativeCalories = 0
        )
        return listOf(zeroPoint) + result
    }

    return result
}

/**
 * 累積カロリーグラフ（Canvas描画）
 */
@Composable
private fun CumulativeCaloriesChart(
    data: List<CumulativeDataPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 最大値
    val maxCumulative = data.maxOfOrNull { it.cumulativeCalories } ?: 0

    // 節目の刻み幅を計算
    val milestoneStep = calculateMilestoneStep(maxCumulative)

    // Y軸の最大値（節目の倍数に切り上げ）
    val yAxisMax = if (maxCumulative == 0) {
        1000
    } else {
        ((maxCumulative / milestoneStep) + 1) * milestoneStep
    }

    // 日付フォーマット（ロケール準拠）
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
    }

    // 数値フォーマット
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    // X軸ラベル用の間引きインデックス（最大8個）
    val xLabelIndices = remember(data.size) {
        if (data.size <= 8) {
            data.indices.toList()
        } else {
            val step = data.size / 7
            (0 until data.size step step).toList().take(8)
        }
    }

    // グラフカード：背景 + 薄い枠線で締める
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.BackgroundMedium)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        if (data.isEmpty() || maxCumulative == 0) {
            // データなし
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.TextSecondary
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartPaddingLeft = 60.dp.toPx()
                val chartPaddingRight = 16.dp.toPx()
                val chartPaddingTop = 20.dp.toPx()
                val chartPaddingBottom = 40.dp.toPx()

                val chartWidth = size.width - chartPaddingLeft - chartPaddingRight
                val chartHeight = size.height - chartPaddingTop - chartPaddingBottom

                // 節目ライン描画
                drawMilestoneLines(
                    milestoneStep = milestoneStep,
                    yAxisMax = yAxisMax,
                    chartPaddingLeft = chartPaddingLeft,
                    chartPaddingTop = chartPaddingTop,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    numberFormat = numberFormat
                )

                // 折れ線グラフ描画
                if (data.size > 1) {
                    val path = Path()
                    data.forEachIndexed { index, point ->
                        val x = chartPaddingLeft + (index.toFloat() / (data.size - 1)) * chartWidth
                        val y = chartPaddingTop + chartHeight - (point.cumulativeCalories.toFloat() / yAxisMax) * chartHeight

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = WorkoutColors.AccentOrange,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // 今日の点と値を描画（強調：半径を大きく）
                if (data.isNotEmpty()) {
                    val lastPoint = data.last()
                    val lastX = chartPaddingLeft + chartWidth
                    val lastY = chartPaddingTop + chartHeight - (lastPoint.cumulativeCalories.toFloat() / yAxisMax) * chartHeight

                    // 点（半径を8dpに強調）
                    drawCircle(
                        color = WorkoutColors.AccentOrange,
                        radius = 8.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )

                    // 今日の累積値ラベル（ドットとの距離を+2dp）
                    val todayLabel = "${numberFormat.format(lastPoint.cumulativeCalories)} kcal"
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FF6B35")
                            textSize = 14.sp.toPx()
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        // ラベル位置（はみ出す場合は左に、ドットとの距離+2dp）
                        val labelX = if (lastX + 80.dp.toPx() > size.width) lastX - 12.dp.toPx() else lastX + 12.dp.toPx()
                        val align = if (lastX + 80.dp.toPx() > size.width) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT
                        paint.textAlign = align
                        drawText(todayLabel, labelX, lastY - 12.dp.toPx(), paint)
                    }
                }

                // X軸ラベル描画
                drawXAxisLabels(
                    data = data,
                    labelIndices = xLabelIndices,
                    dateFormatter = dateFormatter,
                    chartPaddingLeft = chartPaddingLeft,
                    chartPaddingTop = chartPaddingTop,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight
                )
            }
        }
    }
}

/**
 * 節目の刻み幅を計算
 */
private fun calculateMilestoneStep(maxValue: Int): Int {
    return when {
        maxValue < 10_000 -> 1_000
        maxValue < 50_000 -> 5_000
        maxValue < 200_000 -> 10_000
        else -> 50_000
    }
}

/**
 * 節目ライン描画（Y軸ラベルを薄めに）
 */
private fun DrawScope.drawMilestoneLines(
    milestoneStep: Int,
    yAxisMax: Int,
    chartPaddingLeft: Float,
    chartPaddingTop: Float,
    chartWidth: Float,
    chartHeight: Float,
    numberFormat: NumberFormat
) {
    val milestoneColor = Color.Gray.copy(alpha = 0.3f)
    // Y軸ラベルを薄く（alpha 0.6相当）
    val textColor = android.graphics.Color.argb(153, 136, 136, 136) // #888888 with alpha 0.6

    var milestone = 0
    while (milestone <= yAxisMax) {
        val y = chartPaddingTop + chartHeight - (milestone.toFloat() / yAxisMax) * chartHeight

        // 横線
        drawLine(
            color = milestoneColor,
            start = Offset(chartPaddingLeft, y),
            end = Offset(chartPaddingLeft + chartWidth, y),
            strokeWidth = 1.dp.toPx()
        )

        // ラベル（文字サイズを9spに縮小）
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = textColor
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            val label = if (milestone >= 1000) {
                "${milestone / 1000}k"
            } else {
                milestone.toString()
            }
            drawText(label, chartPaddingLeft - 8.dp.toPx(), y + 4.dp.toPx(), paint)
        }

        milestone += milestoneStep
    }
}

/**
 * X軸ラベル描画
 */
private fun DrawScope.drawXAxisLabels(
    data: List<CumulativeDataPoint>,
    labelIndices: List<Int>,
    dateFormatter: DateTimeFormatter,
    chartPaddingLeft: Float,
    chartPaddingTop: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    // X軸ラベルも薄めに
    val textColor = android.graphics.Color.argb(153, 136, 136, 136) // #888888 with alpha 0.6

    labelIndices.forEach { index ->
        if (index < data.size) {
            val point = data[index]
            val x = chartPaddingLeft + (index.toFloat() / max(1, data.size - 1)) * chartWidth

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(
                    point.date.format(dateFormatter),
                    x,
                    chartPaddingTop + chartHeight + 20.dp.toPx(),
                    paint
                )
            }
        }
    }
}

/**
 * グラフ選択入口カード（グラデーション背景）
 */
@Composable
private fun GraphSelectionCard(
    onClick: () -> Unit
) {
    // グラデーション：背景に馴染みつつカードとして存在感が出る
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.BackgroundDark.copy(alpha = 0.95f),
            WorkoutColors.BackgroundMedium.copy(alpha = 0.95f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(brush = gradientBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.which_graph_to_see),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * グラフ選択ダイアログ
 */
@Composable
private fun GraphPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (GraphType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.graph_picker_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 筋トレ（最大kg推移）
                GraphPickerItem(
                    label = stringResource(R.string.graph_picker_strength),
                    onClick = { onSelect(GraphType.STRENGTH) }
                )
                // 有酸素（累積距離）
                GraphPickerItem(
                    label = stringResource(R.string.graph_picker_cardio),
                    onClick = { onSelect(GraphType.CARDIO) }
                )
                // スタジオ（累積回数）
                GraphPickerItem(
                    label = stringResource(R.string.graph_picker_studio),
                    onClick = { onSelect(GraphType.STUDIO) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_cancel))
            }
        }
    )
}

/**
 * グラフ選択アイテム
 */
@Composable
private fun GraphPickerItem(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(WorkoutColors.BackgroundMedium)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = WorkoutColors.TextPrimary
        )
    }
}