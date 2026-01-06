package com.poweder.simpleworkoutlog.ui.dialog

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 */
fun ExerciseEntity.getDisplayName(context: Context): String {
    if (!customName.isNullOrEmpty()) {
        return customName
    }
    if (nameResId != null) {
        return try {
            context.getString(nameResId)
        } catch (e: Exception) {
            name.ifEmpty { "Unknown" }
        }
    }
    return name.ifEmpty { "Unknown" }
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseSlot(
                            name = exercise.getDisplayName(context),
                            isEmpty = false,
                            colorType = colorType,
                            onClick = { onExerciseSelect(exercise) },
                            onEdit = { onRenameExercise(exercise) },
                            onDelete = { exerciseToDelete = exercise }
                        )
                    }

                    val totalSlots = 6
                    val emptyCount = (totalSlots - exercises.size).coerceAtLeast(1)
                    items(emptyCount) {
                        ExerciseSlot(
                            name = stringResource(R.string.empty_slot),
                            isEmpty = true,
                            colorType = colorType,
                            onClick = onAddNewExercise,
                            onEdit = null,
                            onDelete = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
    STRENGTH, CARDIO, INTERVAL, STUDIO
}

@Composable
private fun ExerciseSlot(
    name: String,
    isEmpty: Boolean,
    colorType: ExerciseColorType,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val gradient = if (isEmpty) {
        Brush.horizontalGradient(
            colors = listOf(
                WorkoutColors.BackgroundMedium,
                WorkoutColors.BackgroundDark
            )
        )
    } else {
        when (colorType) {
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
        }
    }

    val borderModifier = if (isEmpty) {
        Modifier.border(1.dp, WorkoutColors.EmptySlotBorder, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .then(borderModifier)
                .background(gradient)
                .clickable { onClick() }
                .padding(vertical = 24.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.Bold,
                color = if (isEmpty) WorkoutColors.TextSecondary else WorkoutColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isEmpty && onEdit != null && onDelete != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = WorkoutColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = WorkoutColors.PureRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp))
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