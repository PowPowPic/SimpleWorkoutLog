package com.poweder.simpleworkoutlog.ui.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.model.SetItem
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import com.poweder.simpleworkoutlog.util.formatWeight
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthTrainingScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val weightUnit by viewModel.weightUnit.collectAsState()
    val adRemoved by viewModel.adRemoved.collectAsState()
    val setItems by viewModel.setItems.collectAsState()
    val sessionTotal by viewModel.sessionTotal.collectAsState()
    val currentExercise by viewModel.currentExercise.collectAsState()

    // 運動時間と消費カロリー入力
    var durationInput by remember { mutableStateOf("") }
    var caloriesInput by remember { mutableStateOf("") }

    // 戻る確認ダイアログ
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

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            WorkoutColors.BackgroundDark,
            WorkoutColors.BackgroundMedium
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
                        viewModel.clearSession()
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
        currentExercise?.let { exercise ->
            Text(
                text = exercise.getDisplayName(context),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // メインコンテンツ
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 運動時間入力
            item {
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.exercise_duration)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = WorkoutColors.TextSecondary
                    )
                )
            }

            // 消費カロリー入力
            item {
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = { caloriesInput = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.exercise_calories)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = WorkoutColors.TextSecondary
                    )
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 種目トータル
            item {
                SessionTotalCard(
                    total = sessionTotal,
                    weightUnit = weightUnit
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // セット一覧
            itemsIndexed(setItems, key = { _, item -> item.id }) { index, setItem ->
                SetItemCard(
                    setItem = setItem,
                    weightUnit = weightUnit,
                    onUpdate = { weight, reps ->
                        viewModel.updateSetItem(setItem.id, weight, reps)
                    },
                    onConfirm = {
                        viewModel.confirmSetItem(setItem.id)
                    },
                    onUnconfirm = {
                        viewModel.unconfirmSetItem(setItem.id)
                    },
                    onDelete = {
                        viewModel.deleteSetItem(setItem.id)
                    }
                )
            }

            // 新しいセットを追加ボタン
            item {
                AddSetButton(
                    onClick = { viewModel.addNewSetItem() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 下部ボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Homeに戻るボタン
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.ButtonCancel)
                    .clickable {
                        if (viewModel.hasUnsavedSets()) {
                            showBackConfirmDialog = true
                        } else {
                            viewModel.clearSession()
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

            // 保存して完了ボタン
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.ButtonConfirm)
                    .clickable {
                        val duration = durationInput.toIntOrNull() ?: 0
                        val calories = caloriesInput.toIntOrNull() ?: 0
                        viewModel.finishAndSave(duration, calories)
                        onBack()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.finish_and_save),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SessionTotalCard(
    total: Double,
    weightUnit: WeightUnit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.StrengthCardStart,
            WorkoutColors.StrengthCardEnd
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .padding(vertical = 16.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.session_total),
                style = MaterialTheme.typography.bodyMedium,
                color = WorkoutColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatWeight(total, weightUnit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary
            )
        }
    }
}

@Composable
private fun SetItemCard(
    setItem: SetItem,
    weightUnit: WeightUnit,
    onUpdate: (Double, Int) -> Unit,
    onConfirm: () -> Unit,
    onUnconfirm: () -> Unit,
    onDelete: () -> Unit
) {
    var weightText by remember(setItem.id, setItem.weight) {
        mutableStateOf(if (setItem.weight > 0) setItem.weight.toString() else "")
    }
    var repsText by remember(setItem.id, setItem.reps) {
        mutableStateOf(if (setItem.reps > 0) setItem.reps.toString() else "")
    }

    val cardGradient = Brush.horizontalGradient(
        colors = if (setItem.isConfirmed) {
            listOf(
                WorkoutColors.ButtonConfirm.copy(alpha = 0.3f),
                WorkoutColors.ButtonConfirm.copy(alpha = 0.2f)
            )
        } else {
            listOf(
                WorkoutColors.StrengthCardStart,
                WorkoutColors.StrengthCardEnd
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardGradient)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // セット番号
            Text(
                text = "${setItem.setNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                modifier = Modifier.width(24.dp)
            )

            // 重量入力
            OutlinedTextField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    val weight = it.toDoubleOrNull() ?: 0.0
                    val reps = repsText.toIntOrNull() ?: 0
                    onUpdate(weight, reps)
                },
                label = { Text(weightUnit.symbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !setItem.isConfirmed,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary,
                    disabledTextColor = WorkoutColors.TextPrimary
                )
            )

            // 回数入力
            OutlinedTextField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    val weight = weightText.toDoubleOrNull() ?: 0.0
                    val reps = it.toIntOrNull() ?: 0
                    onUpdate(weight, reps)
                },
                label = { Text(stringResource(R.string.reps)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !setItem.isConfirmed,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary,
                    disabledTextColor = WorkoutColors.TextPrimary
                )
            )

            // 確定/編集ボタン
            IconButton(
                onClick = {
                    if (setItem.isConfirmed) {
                        onUnconfirm()
                    } else {
                        onConfirm()
                    }
                }
            ) {
                Icon(
                    imageVector = if (setItem.isConfirmed) Icons.Default.Edit else Icons.Default.Check,
                    contentDescription = if (setItem.isConfirmed) "Edit" else "Confirm",
                    tint = if (setItem.isConfirmed) WorkoutColors.AccentOrange else WorkoutColors.ButtonConfirm
                )
            }

            // 削除ボタン
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = WorkoutColors.PureRed
                )
            }
        }
    }
}

@Composable
private fun AddSetButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.BackgroundMedium.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.add_new_set),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.AccentOrange
        )
    }
}