package com.poweder.simpleworkoutlog.ui.dialog

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.data.entity.ExerciseEntity
import com.poweder.simpleworkoutlog.data.entity.WorkoutType
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * ExerciseEntityの表示名を取得する拡張関数
 * 
 * テンプレート種目: templateKey → stringResource で表示名を取得
 * カスタム種目: customName または name を使用
 */
fun ExerciseEntity.getDisplayName(context: Context): String {
    return if (isTemplate) {
        // テンプレート: templateKey から表示名を取得
        val key = templateKey ?: return name.ifEmpty { "Unknown" }
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        if (resId != 0) {
            try {
                context.getString(resId)
            } catch (e: Exception) {
                key // fallback: キー名をそのまま表示
            }
        } else {
            key // fallback: キー名をそのまま表示
        }
    } else {
        // カスタム: customName または name を使用
        customName?.takeIf { it.isNotBlank() } ?: name.ifEmpty { "Unknown" }
    }
}

/**
 * 種目選択ダイアログ（編集/削除アイコン付き）
 */
@Composable
fun ExerciseSelectDialog(
    exercises: List<ExerciseEntity>,
    workoutType: String,
    onExerciseSelect: (ExerciseEntity) -> Unit,
    onAddNewExercise: () -> Unit,
    onRenameExercise: (ExerciseEntity) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    
    exerciseToDelete?.let { exercise ->
        DeleteConfirmDialog(
            itemName = exercise.getDisplayName(context),
            onConfirm = {
                onDeleteExercise(exercise)
                exerciseToDelete = null
            },
            onDismiss = { exerciseToDelete = null }
        )
    }
    
    val colorType = when (workoutType) {
        WorkoutType.STRENGTH -> ExerciseColorType.STRENGTH
        WorkoutType.CARDIO -> ExerciseColorType.CARDIO
        WorkoutType.INTERVAL -> ExerciseColorType.INTERVAL
        WorkoutType.STUDIO -> ExerciseColorType.STUDIO
        WorkoutType.OTHER -> ExerciseColorType.OTHER
        else -> ExerciseColorType.CARDIO
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WorkoutColors.BackgroundDark)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.select_exercise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 種目がある場合のみグリッド表示
                if (exercises.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 450.dp)
                    ) {
                        items(
                            items = exercises,
                            key = { it.id }
                        ) { exercise ->
                            ExerciseSlot(
                                name = exercise.getDisplayName(context),
                                colorType = colorType,
                                onClick = { onExerciseSelect(exercise) },
                                onEdit = { onRenameExercise(exercise) },
                                onDelete = { exerciseToDelete = exercise }
                            )
                        }
                    }
                } else {
                    // 種目がない場合のメッセージ
                    Text(
                        text = stringResource(R.string.no_exercises_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WorkoutColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // + 新規種目を追加 ボタン
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WorkoutColors.AccentOrange.copy(alpha = 0.2f))
                        .clickable { onAddNewExercise() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.add_new_exercise),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutColors.AccentOrange
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        stringResource(R.string.common_cancel),
                        color = WorkoutColors.TextSecondary
                    )
                }
            }
        }
    }
}

private enum class ExerciseColorType {
    STRENGTH, CARDIO, INTERVAL, STUDIO, OTHER
}

@Composable
private fun ExerciseSlot(
    name: String,
    colorType: ExerciseColorType,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val gradient = when (colorType) {
        ExerciseColorType.STRENGTH -> Brush.horizontalGradient(
            colors = listOf(WorkoutColors.StrengthCardStart, WorkoutColors.StrengthCardEnd)
        )
        ExerciseColorType.CARDIO -> Brush.horizontalGradient(
            colors = listOf(WorkoutColors.CardioCardStart, WorkoutColors.CardioCardEnd)
        )
        ExerciseColorType.INTERVAL -> Brush.horizontalGradient(
            colors = listOf(WorkoutColors.IntervalCardStart, WorkoutColors.IntervalCardEnd)
        )
        ExerciseColorType.STUDIO -> Brush.horizontalGradient(
            colors = listOf(WorkoutColors.StudioCardStart, WorkoutColors.StudioCardEnd)
        )
        ExerciseColorType.OTHER -> Brush.horizontalGradient(
            colors = listOf(WorkoutColors.OtherCardStart, WorkoutColors.OtherCardEnd)
        )
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(gradient)
                .clickable { onClick() }
                .padding(vertical = 24.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 編集/削除アイコン（常に表示）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = WorkoutColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = WorkoutColors.PureRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddExerciseDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_new_exercise)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.exercise_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
fun RenameDialog(currentName: String, title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.exercise_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (newName.isNotBlank()) onConfirm(newName.trim()) }, enabled = newName.isNotBlank()) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
fun DeleteConfirmDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_message, itemName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete), color = WorkoutColors.PureRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
