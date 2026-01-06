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
    
    // 戻る確認ダイアログ
    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = { Text(stringResource(R.string.discard_confirm_title)) },
            text = { Text(stringResource(R.string.discard_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackConfirmDialog = false
                        viewModel.clearSession()
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
    
    // 背景グラデーション
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
        
        // ヘッダー（種目名）
        Text(
            text = currentExercise?.getDisplayName(context) ?: stringResource(R.string.workout_strength),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // セット一覧（LazyColumn）
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = setItems,
                key = { _, item -> item.id }
            ) { index, setItem ->
                SetInputRow(
                    setItem = setItem,
                    weightUnit = weightUnit,
                    onWeightChange = { weight ->
                        viewModel.updateSetItem(setItem.id, weight, setItem.reps)
                    },
                    onRepsChange = { reps ->
                        viewModel.updateSetItem(setItem.id, setItem.weight, reps)
                    },
                    onConfirm = { viewModel.confirmSetItem(setItem.id) },
                    onEdit = { viewModel.unconfirmSetItem(setItem.id) },
                    onDelete = { viewModel.deleteSetItem(setItem.id) }
                )
            }
            
            // ＋ Add New Set ボタン
            item {
                Spacer(modifier = Modifier.height(8.dp))
                AddNewSetButton(
                    onClick = { viewModel.addNewSetItem() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // 種目トータル
        SessionTotalBar(
            total = sessionTotal,
            weightUnit = weightUnit
        )
        
        // 下部ボタン：Homeに戻る / Finish & Save
        BottomActionButtons(
            onGoHome = {
                if (viewModel.hasUnsavedSets()) {
                    showBackConfirmDialog = true
                } else {
                    viewModel.clearSession()
                    onBack()
                }
            },
            onFinishAndSave = {
                viewModel.finishAndSave()
                onBack()
            }
        )
        
        // 設定案内
        Text(
            text = stringResource(R.string.settings_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = WorkoutColors.PureBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}

/**
 * セット入力行
 */
@Composable
private fun SetInputRow(
    setItem: SetItem,
    weightUnit: WeightUnit,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
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
                WorkoutColors.StrengthCardStart.copy(alpha = 0.5f),
                WorkoutColors.StrengthCardEnd.copy(alpha = 0.5f)
            )
        } else {
            listOf(
                WorkoutColors.StrengthCardStart,
                WorkoutColors.StrengthCardEnd,
                WorkoutColors.StrengthCardStart
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
                text = "${setItem.setNumber}.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                modifier = Modifier.width(28.dp)
            )
            
            if (setItem.isConfirmed) {
                // 確定済み：表示のみ
                Text(
                    text = "${formatWeight(setItem.weight, weightUnit)} × ${setItem.reps}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                
                // トータル
                Text(
                    text = "→ ${formatWeight(setItem.totalWeight, weightUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.AccentOrange
                )
                
                // 編集ボタン
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = WorkoutColors.AccentOrangeLight
                    )
                }
            } else {
                // 未確定：入力フィールド
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { 
                        val filtered = it.filter { c -> c.isDigit() || c == '.' }
                        weightText = filtered
                        filtered.toDoubleOrNull()?.let { w -> onWeightChange(w) }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(weightUnit.symbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = WorkoutColors.TextSecondary,
                        focusedLabelColor = WorkoutColors.AccentOrange,
                        unfocusedLabelColor = WorkoutColors.TextSecondary
                    )
                )
                
                Text(
                    text = "×",
                    style = MaterialTheme.typography.titleMedium,
                    color = WorkoutColors.TextPrimary
                )
                
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { 
                        val filtered = it.filter { c -> c.isDigit() }
                        repsText = filtered
                        filtered.toIntOrNull()?.let { r -> onRepsChange(r) }
                    },
                    modifier = Modifier.weight(0.7f),
                    label = { Text(stringResource(R.string.reps)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary,
                        focusedBorderColor = WorkoutColors.AccentOrange,
                        unfocusedBorderColor = WorkoutColors.TextSecondary,
                        focusedLabelColor = WorkoutColors.AccentOrange,
                        unfocusedLabelColor = WorkoutColors.TextSecondary
                    )
                )
                
                // 確定ボタン
                IconButton(
                    onClick = onConfirm,
                    enabled = setItem.isValid,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.confirm),
                        tint = if (setItem.isValid) WorkoutColors.AccentOrange else WorkoutColors.TextSecondary
                    )
                }
            }
            
            // 削除ボタン（常に表示）
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = WorkoutColors.PureRed
                )
            }
        }
    }
}

/**
 * ＋ Add New Set ボタン
 */
@Composable
private fun AddNewSetButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WorkoutColors.AccentOrange)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.add_new_set),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary
        )
    }
}

/**
 * 種目トータル表示バー
 */
@Composable
private fun SessionTotalBar(
    total: Double,
    weightUnit: WeightUnit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkoutColors.GrandTotalBackground)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.session_total),
                style = MaterialTheme.typography.titleMedium,
                color = WorkoutColors.TextSecondary
            )
            Text(
                text = formatWeight(total, weightUnit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.GrandTotalText
            )
        }
    }
}

/**
 * 下部アクションボタン
 */
@Composable
private fun BottomActionButtons(
    onGoHome: () -> Unit,
    onFinishAndSave: () -> Unit
) {
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
                .clickable { onGoHome() }
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
        
        // Finish & Save ボタン
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(WorkoutColors.AccentOrange)
                .clickable { onFinishAndSave() }
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
}
