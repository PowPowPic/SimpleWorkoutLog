package com.poweder.simpleworkoutlog.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.ui.dialog.AddExerciseDialog
import com.poweder.simpleworkoutlog.ui.dialog.ExerciseSelectDialog
import com.poweder.simpleworkoutlog.ui.dialog.RenameDialog
import com.poweder.simpleworkoutlog.ui.dialog.WorkoutTypeSelectDialog
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.interval.IntervalExerciseSelectDialog
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import com.poweder.simpleworkoutlog.util.formatCalories
import com.poweder.simpleworkoutlog.util.formatHms
import com.poweder.simpleworkoutlog.util.formatWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: WorkoutViewModel,
    onSettingsClick: () -> Unit,
    onNavigateToStrength: () -> Unit,
    onNavigateToCardio: () -> Unit,
    onNavigateToInterval: () -> Unit,
    onNavigateToStudio: () -> Unit = {},
    onNavigateToOther: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val todayTotalWeight by viewModel.todayTotalWeight.collectAsState()
    val todayTotalDurationSeconds by viewModel.todayTotalDurationSeconds.collectAsState()
    val todayTotalCalories by viewModel.todayTotalCalories.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val strengthExercises by viewModel.strengthExercises.collectAsState()
    val cardioExercises by viewModel.cardioExercises.collectAsState()
    val studioExercises by viewModel.studioExercises.collectAsState()
    val otherExercises by viewModel.otherExercises.collectAsState()

    var showWorkoutTypeDialog by remember { mutableStateOf(false) }

    // 過去のトレーニング追加用
    var showPastWorkoutTypeDialog by remember { mutableStateOf(false) }
    var selectedPastDate by remember { mutableStateOf<LocalDate?>(null) }

    // 筋トレ用ダイアログ
    var showStrengthExerciseDialog by remember { mutableStateOf(false) }
    var showAddStrengthExerciseDialog by remember { mutableStateOf(false) }

    // 有酸素用ダイアログ
    var showCardioExerciseDialog by remember { mutableStateOf(false) }
    var showAddCardioExerciseDialog by remember { mutableStateOf(false) }

    // インターバル用ダイアログ（専用：HIIT/TABATAのみ）
    var showIntervalExerciseDialog by remember { mutableStateOf(false) }

    // スタジオ用ダイアログ
    var showStudioExerciseDialog by remember { mutableStateOf(false) }
    var showAddStudioExerciseDialog by remember { mutableStateOf(false) }

    // その他用ダイアログ
    var showOtherExerciseDialog by remember { mutableStateOf(false) }
    var showAddOtherExerciseDialog by remember { mutableStateOf(false) }

    // 名前変更用
    var exerciseToRename by remember { mutableStateOf<ExerciseEntity?>(null) }
    var currentWorkoutTypeForRename by remember { mutableStateOf<String?>(null) }

    // 日付をライフサイクルに連動して更新
    var displayDate by remember { mutableStateOf(LocalDate.now()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                displayDate = LocalDate.now()
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

    // 名前変更ダイアログ
    exerciseToRename?.let { exercise ->
        RenameDialog(
            currentName = exercise.getDisplayName(context),
            title = stringResource(R.string.rename),
            onConfirm = { newName ->
                // リネーム時はカスタム名を設定し、テンプレートフラグをOFFに
                viewModel.updateExercise(exercise.copy(
                    customName = newName,
                    isTemplate = false,
                    templateKey = null
                ))
                exerciseToRename = null
                when (currentWorkoutTypeForRename) {
                    WorkoutType.STRENGTH -> showStrengthExerciseDialog = true
                    WorkoutType.CARDIO -> showCardioExerciseDialog = true
                    WorkoutType.STUDIO -> showStudioExerciseDialog = true
                    WorkoutType.OTHER -> showOtherExerciseDialog = true
                }
            },
            onDismiss = {
                exerciseToRename = null
                when (currentWorkoutTypeForRename) {
                    WorkoutType.STRENGTH -> showStrengthExerciseDialog = true
                    WorkoutType.CARDIO -> showCardioExerciseDialog = true
                    WorkoutType.STUDIO -> showStudioExerciseDialog = true
                    WorkoutType.OTHER -> showOtherExerciseDialog = true
                }
            }
        )
    }

    // 運動種別選択ダイアログ
    if (showWorkoutTypeDialog) {
        WorkoutTypeSelectDialog(
            onStrengthSelect = {
                showWorkoutTypeDialog = false
                showStrengthExerciseDialog = true
            },
            onCardioSelect = {
                showWorkoutTypeDialog = false
                showCardioExerciseDialog = true
            },
            onIntervalSelect = {
                showWorkoutTypeDialog = false
                showIntervalExerciseDialog = true
            },
            onStudioSelect = {
                showWorkoutTypeDialog = false
                showStudioExerciseDialog = true
            },
            onOtherSelect = {
                showWorkoutTypeDialog = false
                showOtherExerciseDialog = true
            },
            onDismiss = { showWorkoutTypeDialog = false }
        )
    }

    // 過去のトレーニング用：運動種別選択ダイアログ
    if (showPastWorkoutTypeDialog && selectedPastDate != null) {
        WorkoutTypeSelectDialog(
            onStrengthSelect = {
                showPastWorkoutTypeDialog = false
                selectedPastDate?.let { date ->
                    viewModel.setTargetDate(date)
                }
                showStrengthExerciseDialog = true
            },
            onCardioSelect = {
                showPastWorkoutTypeDialog = false
                selectedPastDate?.let { date ->
                    viewModel.setTargetDate(date)
                }
                showCardioExerciseDialog = true
            },
            onIntervalSelect = {
                showPastWorkoutTypeDialog = false
                selectedPastDate?.let { date ->
                    viewModel.setTargetDate(date)
                }
                showIntervalExerciseDialog = true
            },
            onStudioSelect = {
                showPastWorkoutTypeDialog = false
                selectedPastDate?.let { date ->
                    viewModel.setTargetDate(date)
                }
                showStudioExerciseDialog = true
            },
            onOtherSelect = {
                showPastWorkoutTypeDialog = false
                selectedPastDate?.let { date ->
                    viewModel.setTargetDate(date)
                }
                showOtherExerciseDialog = true
            },
            onDismiss = {
                showPastWorkoutTypeDialog = false
                selectedPastDate = null
            }
        )
    }

    // 筋トレ種目選択ダイアログ
    if (showStrengthExerciseDialog) {
        ExerciseSelectDialog(
            exercises = strengthExercises,
            workoutType = WorkoutType.STRENGTH,
            onExerciseSelect = { exercise ->
                showStrengthExerciseDialog = false
                viewModel.setCurrentExercise(WorkoutType.STRENGTH, exercise)
                viewModel.initializeSetItems(exercise.id)
                onNavigateToStrength()
            },
            onAddNewExercise = {
                showStrengthExerciseDialog = false
                showAddStrengthExerciseDialog = true
            },
            onRenameExercise = { exercise ->
                showStrengthExerciseDialog = false
                exerciseToRename = exercise
                currentWorkoutTypeForRename = WorkoutType.STRENGTH
            },
            onDeleteExercise = { exercise ->
                viewModel.deleteExercise(exercise.id)
            },
            onDismiss = { showStrengthExerciseDialog = false },
            onReorderExercises = { reorderedList ->
                viewModel.updateExerciseSortOrders(reorderedList)
            }
        )
    }

    // 筋トレ種目追加ダイアログ
    if (showAddStrengthExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name ->
                viewModel.addExercise(name, WorkoutType.STRENGTH)
                showAddStrengthExerciseDialog = false
                showStrengthExerciseDialog = true
            },
            onDismiss = {
                showAddStrengthExerciseDialog = false
                showStrengthExerciseDialog = true
            }
        )
    }

    // 有酸素種目選択ダイアログ
    if (showCardioExerciseDialog) {
        ExerciseSelectDialog(
            exercises = cardioExercises,
            workoutType = WorkoutType.CARDIO,
            onExerciseSelect = { exercise ->
                showCardioExerciseDialog = false
                viewModel.setCurrentExercise(WorkoutType.CARDIO, exercise)
                onNavigateToCardio()
            },
            onAddNewExercise = {
                showCardioExerciseDialog = false
                showAddCardioExerciseDialog = true
            },
            onRenameExercise = { exercise ->
                showCardioExerciseDialog = false
                exerciseToRename = exercise
                currentWorkoutTypeForRename = WorkoutType.CARDIO
            },
            onDeleteExercise = { exercise ->
                viewModel.deleteExercise(exercise.id)
            },
            onDismiss = { showCardioExerciseDialog = false },
            onReorderExercises = { reorderedList ->
                viewModel.updateExerciseSortOrders(reorderedList)
            }
        )
    }

    // 有酸素種目追加ダイアログ
    if (showAddCardioExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name ->
                viewModel.addExercise(name, WorkoutType.CARDIO)
                showAddCardioExerciseDialog = false
                showCardioExerciseDialog = true
            },
            onDismiss = {
                showAddCardioExerciseDialog = false
                showCardioExerciseDialog = true
            }
        )
    }

    // インターバル種目選択ダイアログ（専用）
    if (showIntervalExerciseDialog) {
        IntervalExerciseSelectDialog(
            onTabataSelect = {
                showIntervalExerciseDialog = false
                viewModel.setIntervalExerciseName("TABATA")
                onNavigateToInterval()
            },
            onHiitSelect = {
                showIntervalExerciseDialog = false
                viewModel.setIntervalExerciseName("HIIT")
                onNavigateToInterval()
            },
            onEmomSelect = {
                showIntervalExerciseDialog = false
                viewModel.setIntervalExerciseName("EMOM")
                onNavigateToInterval()
            },
            onDismiss = { showIntervalExerciseDialog = false }
        )
    }

    // スタジオ種目選択ダイアログ
    if (showStudioExerciseDialog) {
        ExerciseSelectDialog(
            exercises = studioExercises,
            workoutType = WorkoutType.STUDIO,
            onExerciseSelect = { exercise ->
                showStudioExerciseDialog = false
                viewModel.setCurrentExercise(WorkoutType.STUDIO, exercise)
                onNavigateToStudio()
            },
            onAddNewExercise = {
                showStudioExerciseDialog = false
                showAddStudioExerciseDialog = true
            },
            onRenameExercise = { exercise ->
                showStudioExerciseDialog = false
                exerciseToRename = exercise
                currentWorkoutTypeForRename = WorkoutType.STUDIO
            },
            onDeleteExercise = { exercise ->
                viewModel.deleteExercise(exercise.id)
            },
            onDismiss = { showStudioExerciseDialog = false },
            onReorderExercises = { reorderedList ->
                viewModel.updateExerciseSortOrders(reorderedList)
            }
        )
    }

    // スタジオ種目追加ダイアログ
    if (showAddStudioExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name ->
                viewModel.addExercise(name, WorkoutType.STUDIO)
                showAddStudioExerciseDialog = false
                showStudioExerciseDialog = true
            },
            onDismiss = {
                showAddStudioExerciseDialog = false
                showStudioExerciseDialog = true
            }
        )
    }

    // その他種目選択ダイアログ
    if (showOtherExerciseDialog) {
        ExerciseSelectDialog(
            exercises = otherExercises,
            workoutType = WorkoutType.OTHER,
            onExerciseSelect = { exercise ->
                showOtherExerciseDialog = false
                viewModel.setCurrentExercise(WorkoutType.OTHER, exercise)
                onNavigateToOther()
            },
            onAddNewExercise = {
                showOtherExerciseDialog = false
                showAddOtherExerciseDialog = true
            },
            onRenameExercise = { exercise ->
                showOtherExerciseDialog = false
                exerciseToRename = exercise
                currentWorkoutTypeForRename = WorkoutType.OTHER
            },
            onDeleteExercise = { exercise ->
                viewModel.deleteExercise(exercise.id)
            },
            onDismiss = { showOtherExerciseDialog = false },
            onReorderExercises = { reorderedList ->
                viewModel.updateExerciseSortOrders(reorderedList)
            }
        )
    }

    // その他種目追加ダイアログ
    if (showAddOtherExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name ->
                viewModel.addExercise(name, WorkoutType.OTHER)
                showAddOtherExerciseDialog = false
                showOtherExerciseDialog = true
            },
            onDismiss = {
                showAddOtherExerciseDialog = false
                showOtherExerciseDialog = true
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // ① 日付表示
        // フォントサイズを変更したい場合は fontSize = XX.sp の数値を調整してください
        // （例：16.sp → 小さめ、20.sp → 大きめ）
        // style も bodySmall → bodyMedium / titleSmall / titleMedium などに変更可能です
        Text(
            text = displayDate.format(dateFormatter),
            style = MaterialTheme.typography.titleSmall,  // ① 変更前: bodySmall
            fontSize = 18.sp,                             // ① 日付フォントサイズ（調整ポイント）
            fontWeight = FontWeight.Medium,               // ① 少し太めにして視認性UP
            color = WorkoutColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // スクロール可能なコンテンツエリア（高齢者対応）
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 今日の運動時間カード（h:mm:ss形式）
            TodaySummaryCard(
                title = stringResource(R.string.today_duration),
                value = formatHms(todayTotalDurationSeconds)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 今日の消費カロリーカード
            TodaySummaryCard(
                title = stringResource(R.string.today_calories),
                value = formatCalories(todayTotalCalories)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Today Grand Total（挙上重量）
            GrandTotalCard(
                totalWeight = todayTotalWeight,
                weightUnit = weightUnit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 今日のトレーニングメニューカード
            MainActionCard(
                text = stringResource(R.string.card_today_menu),
                onClick = { showWorkoutTypeDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 深夜ルール説明
            Text(
                text = stringResource(R.string.midnight_rule_note),
                style = MaterialTheme.typography.bodySmall,
                color = WorkoutColors.TextSecondary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 過去のトレーニングを追加ボタン
            PastWorkoutButton(
                onClick = {
                    // 日付選択ダイアログを表示（月曜始まり）
                    val today = currentLogicalDate()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selected = LocalDate.of(year, month + 1, dayOfMonth)
                            // 未来の日付は選択不可
                            if (!selected.isAfter(today)) {
                                selectedPastDate = selected
                                showPastWorkoutTypeDialog = true
                            }
                        },
                        today.year,
                        today.monthValue - 1,
                        today.dayOfMonth
                    ).apply {
                        // カレンダーを月曜始まりに設定
                        datePicker.firstDayOfWeek = Calendar.MONDAY
                        // 未来の日付を選択不可に
                        datePicker.maxDate = System.currentTimeMillis()
                    }.show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 設定案内（青文字、クリック可能）
            Text(
                text = stringResource(R.string.settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = WorkoutColors.PureBlue,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSettingsClick() }
                    .padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * 今日のサマリーカード（運動時間、消費カロリー用）
 *
 * ② カードの高さ調整ポイント：padding(vertical = XX.dp) の数値を変更してください
 *    現在: 8.dp（変更前: 12.dp）
 *    小さくしたい → 数値を下げる（例: 6.dp）
 *    大きくしたい → 数値を上げる（例: 10.dp）
 *
 * ③ グラデーション調整ポイント：
 *    SummaryCardGradient の色定義を WorkoutColors に追加するか、
 *    直接 colors = listOf(Color(...), Color(...)) で調整してください
 *    centerColor のアルファ値（0.15f）を上げるとグラデーションが濃くなります
 */
@Composable
private fun TodaySummaryCard(
    title: String,
    value: String
) {
    // ③ サマリーカード用グラデーション（運動時間・消費カロリー）
    // 両端を WorkoutColors.GrandTotalBackground（既存色）、
    // 中央をアクセントカラー（やや明るい）にすることでグラデーションを際立たせています
    // centerColor のアルファ値（0.15f）を上げると中央がより明るくなります（調整ポイント）
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.GrandTotalBackground,
            WorkoutColors.GrandTotalBackground.copy(alpha = 0.15f)  // ③ 中央の明るさ（調整ポイント）
                .let { Color(
                    red   = (WorkoutColors.GrandTotalBackground.red   * 0.7f + 0.30f).coerceIn(0f, 1f),
                    green = (WorkoutColors.GrandTotalBackground.green * 0.7f + 0.30f).coerceIn(0f, 1f),
                    blue  = (WorkoutColors.GrandTotalBackground.blue  * 0.7f + 0.55f).coerceIn(0f, 1f),
                    alpha = 1f
                )},
            WorkoutColors.GrandTotalBackground
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)                          // ③ 変更前: .background(WorkoutColors.GrandTotalBackground)
            .padding(vertical = 8.dp, horizontal = 16.dp), // ② 縦パディング（調整ポイント）変更前: 12.dp
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = WorkoutColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.GrandTotalText
            )
        }
    }
}

/**
 * ② カードの高さ調整ポイント：padding(vertical = XX.dp) の数値を変更してください
 *    現在: 12.dp（変更前: 20.dp）
 *    小さくしたい → 数値を下げる（例: 8.dp）
 *    大きくしたい → 数値を上げる（例: 16.dp）
 *
 * ③ グラデーション調整ポイント：
 *    TodaySummaryCard と同じ方式。
 *    中央色の blue 成分（0.55f）を上げると青みが強くなります
 */
@Composable
private fun GrandTotalCard(
    totalWeight: Double,
    weightUnit: WeightUnit
) {
    // ③ グランドトータルカード用グラデーション（挙上重量）
    // TodaySummaryCard と同じロジックで中央を少し明るく表示
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.GrandTotalBackground,
            Color(
                red   = (WorkoutColors.GrandTotalBackground.red   * 0.7f + 0.30f).coerceIn(0f, 1f),
                green = (WorkoutColors.GrandTotalBackground.green * 0.7f + 0.30f).coerceIn(0f, 1f),
                blue  = (WorkoutColors.GrandTotalBackground.blue  * 0.7f + 0.55f).coerceIn(0f, 1f),
                alpha = 1f
            ),                                                   // ③ 中央色（調整ポイント）
            WorkoutColors.GrandTotalBackground
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)                           // ③ 変更前: .background(WorkoutColors.GrandTotalBackground)
            .padding(vertical = 12.dp, horizontal = 16.dp), // ② 縦パディング（調整ポイント）変更前: 20.dp
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.today_grand_total),
                style = MaterialTheme.typography.titleMedium,
                color = WorkoutColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatWeight(totalWeight, weightUnit),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.GrandTotalText
            )
        }
    }
}

/**
 * 今日のトレーニングメニューカード（グラデーション済み）
 *
 * ② カードの高さ調整ポイント：padding(vertical = XX.dp) の数値を変更してください
 *    現在: 18.dp（変更前: 28.dp）
 *    小さくしたい → 数値を下げる（例: 14.dp）
 *    大きくしたい → 数値を上げる（例: 22.dp）
 */
@Composable
private fun MainActionCard(
    text: String,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.MainCardStart,
            WorkoutColors.StrengthCardEnd,
            WorkoutColors.MainCardStart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)  // 最低タップサイズ保証（高齢者対応）
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(vertical = 18.dp),                    // ② 縦パディング（調整ポイント）変更前: 28.dp
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            maxLines = 2  // 高齢者対応：文字が大きくても2行まで表示
        )
    }
}

/**
 * 過去のトレーニングを追加ボタン
 * ※ このボタンは修正対象外です（高さ変更なし）
 */
@Composable
private fun PastWorkoutButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)  // 最低タップサイズ保証（高齢者対応）
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.GrandTotalBackground)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.add_past_workout),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = WorkoutColors.TextSecondary,
            maxLines = 2  // 高齢者対応：文字が大きくても2行まで表示
        )
    }
}