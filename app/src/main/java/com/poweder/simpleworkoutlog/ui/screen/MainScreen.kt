package com.poweder.simpleworkoutlog.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.AddExerciseDialog
import com.poweder.simpleworkoutlog.ui.dialog.ExerciseSelectDialog
import com.poweder.simpleworkoutlog.ui.dialog.RenameDialog
import com.poweder.simpleworkoutlog.ui.dialog.WorkoutTypeSelectDialog
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import com.poweder.simpleworkoutlog.util.formatWeight
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: WorkoutViewModel,
    onSettingsClick: () -> Unit,
    onNavigateToStrength: () -> Unit,
    onNavigateToCardio: () -> Unit,
    onNavigateToInterval: () -> Unit,
    onNavigateToStudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val todayTotalWeight by viewModel.todayTotalWeight.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val adRemoved by viewModel.adRemoved.collectAsState()
    val strengthExercises by viewModel.strengthExercises.collectAsState()
    val cardioExercises by viewModel.cardioExercises.collectAsState()
    val intervalExercises by viewModel.intervalExercises.collectAsState()
    val studioExercises by viewModel.studioExercises.collectAsState()

    var showWorkoutTypeDialog by remember { mutableStateOf(false) }
    var showAddWorkoutTypeDialog by remember { mutableStateOf(false) }

    // 筋トレ用ダイアログ
    var showStrengthExerciseDialog by remember { mutableStateOf(false) }
    var showAddStrengthExerciseDialog by remember { mutableStateOf(false) }

    // 有酸素用ダイアログ
    var showCardioExerciseDialog by remember { mutableStateOf(false) }
    var showAddCardioExerciseDialog by remember { mutableStateOf(false) }

    // インターバル用ダイアログ
    var showIntervalExerciseDialog by remember { mutableStateOf(false) }
    var showAddIntervalExerciseDialog by remember { mutableStateOf(false) }

    // スタジオ用ダイアログ
    var showStudioExerciseDialog by remember { mutableStateOf(false) }
    var showAddStudioExerciseDialog by remember { mutableStateOf(false) }

    // 名前変更用
    var exerciseToRename by remember { mutableStateOf<ExerciseEntity?>(null) }
    var currentWorkoutTypeForRename by remember { mutableStateOf<String?>(null) }

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

    // ロケールに応じた日付フォーマット
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
    }

    // ワークアウトタイプ追加ダイアログ（Coming Soon）
    if (showAddWorkoutTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddWorkoutTypeDialog = false },
            title = { Text(stringResource(R.string.add_workout_type)) },
            text = {
                Text(
                    text = stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showAddWorkoutTypeDialog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    // 名前変更ダイアログ
    exerciseToRename?.let { exercise ->
        RenameDialog(
            currentName = exercise.getDisplayName(context),
            title = stringResource(R.string.rename),
            onConfirm = { newName ->
                viewModel.updateExercise(exercise.copy(customName = newName, nameResId = null))
                exerciseToRename = null
                // 元のダイアログに戻る
                when (currentWorkoutTypeForRename) {
                    WorkoutType.STRENGTH -> showStrengthExerciseDialog = true
                    WorkoutType.CARDIO -> showCardioExerciseDialog = true
                    WorkoutType.INTERVAL -> showIntervalExerciseDialog = true
                    WorkoutType.STUDIO -> showStudioExerciseDialog = true
                }
            },
            onDismiss = {
                exerciseToRename = null
                when (currentWorkoutTypeForRename) {
                    WorkoutType.STRENGTH -> showStrengthExerciseDialog = true
                    WorkoutType.CARDIO -> showCardioExerciseDialog = true
                    WorkoutType.INTERVAL -> showIntervalExerciseDialog = true
                    WorkoutType.STUDIO -> showStudioExerciseDialog = true
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
            onAddNewType = {
                showWorkoutTypeDialog = false
                showAddWorkoutTypeDialog = true
            },
            onDismiss = { showWorkoutTypeDialog = false }
        )
    }

    // 筋トレ種目選択ダイアログ
    if (showStrengthExerciseDialog) {
        ExerciseSelectDialog(
            exercises = strengthExercises,
            workoutType = WorkoutType.STRENGTH,
            onExerciseSelect = { exercise ->
                showStrengthExerciseDialog = false
                viewModel.setCurrentExercise(exercise)
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
            onDismiss = { showStrengthExerciseDialog = false }
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
                viewModel.setCurrentExercise(exercise)
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
            onDismiss = { showCardioExerciseDialog = false }
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

    // インターバル種目選択ダイアログ
    if (showIntervalExerciseDialog) {
        ExerciseSelectDialog(
            exercises = intervalExercises,
            workoutType = WorkoutType.INTERVAL,
            onExerciseSelect = { exercise ->
                showIntervalExerciseDialog = false
                viewModel.setCurrentExercise(exercise)
                onNavigateToInterval()
            },
            onAddNewExercise = {
                showIntervalExerciseDialog = false
                showAddIntervalExerciseDialog = true
            },
            onRenameExercise = { exercise ->
                showIntervalExerciseDialog = false
                exerciseToRename = exercise
                currentWorkoutTypeForRename = WorkoutType.INTERVAL
            },
            onDeleteExercise = { exercise ->
                viewModel.deleteExercise(exercise.id)
            },
            onDismiss = { showIntervalExerciseDialog = false }
        )
    }

    // インターバル種目追加ダイアログ
    if (showAddIntervalExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name ->
                viewModel.addExercise(name, WorkoutType.INTERVAL)
                showAddIntervalExerciseDialog = false
                showIntervalExerciseDialog = true
            },
            onDismiss = {
                showAddIntervalExerciseDialog = false
                showIntervalExerciseDialog = true
            }
        )
    }

    // スタジオ種目選択ダイアログ
    if (showStudioExerciseDialog) {
        ExerciseSelectDialog(
            exercises = studioExercises,
            workoutType = WorkoutType.STUDIO,
            onExerciseSelect = { exercise ->
                showStudioExerciseDialog = false
                viewModel.setCurrentExercise(exercise)
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
            onDismiss = { showStudioExerciseDialog = false }
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

    // 背景グラデーション（左→右：濃→薄）
    val backgroundGradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.BackgroundDark,
            WorkoutColors.BackgroundMedium
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // 広告バナー（または余白）
        TopBannerAd(showAd = !adRemoved)

        // 日付表示（ロケール対応、小さめフォント）
        Text(
            text = logicalDate.format(dateFormatter),
            style = MaterialTheme.typography.bodySmall,
            color = WorkoutColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Today Grand Total
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

            Spacer(modifier = Modifier.weight(1f))

            // 設定案内（青文字、少し大きめ）
            Text(
                text = stringResource(R.string.settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = WorkoutColors.PureBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun GrandTotalCard(
    totalWeight: Double,
    weightUnit: WeightUnit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.GrandTotalBackground)
            .padding(vertical = 20.dp, horizontal = 16.dp),
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

@Composable
private fun MainActionCard(
    text: String,
    onClick: () -> Unit
) {
    // スタイリッシュなグラデーション（左→右：濃→薄）
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.StrengthCardStart,
            WorkoutColors.StrengthCardEnd,
            WorkoutColors.StrengthCardStart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary
        )
    }
}