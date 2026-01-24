package com.poweder.simpleworkoutlog.ui.dialog

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
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

// 編集アイコンの色（真っ青）
private val EditIconColor = Color(0xFF0000FF)

/**
 * 種目選択ダイアログ（編集/削除アイコン付き、並び替え対応）
 */
@Composable
fun ExerciseSelectDialog(
    exercises: List<ExerciseEntity>,
    workoutType: String,
    onExerciseSelect: (ExerciseEntity) -> Unit,
    onAddNewExercise: () -> Unit,
    onRenameExercise: (ExerciseEntity) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit,
    onReorderExercises: (List<ExerciseEntity>) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    // 並び替え用のローカルリスト
    var reorderedExercises by remember(exercises) { mutableStateOf(exercises) }

    // ドラッグ状態（indexではなくIDで管理する：これが重要）
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // アイテムの高さ（dp）
    val itemHeightDp = 64.dp
    val itemSpacingDp = 8.dp
    val totalItemHeightDp = itemHeightDp + itemSpacingDp

    // LazyListState
    val listState = rememberLazyListState()

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
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 並び替えヒント
                if (reorderedExercises.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.drag_to_reorder),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF0000),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 種目がある場合はリスト表示
                if (reorderedExercises.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(itemSpacingDp),
                        modifier = Modifier.heightIn(max = 450.dp),
                        // ドラッグ中はスクロールを無効化
                        userScrollEnabled = draggedItemId == null
                    ) {
                        itemsIndexed(
                            items = reorderedExercises,
                            key = { _, exercise -> exercise.id }
                        ) { index, exercise ->
                            val isDragging = draggedItemId == exercise.id

                            // ドラッグ中のアイテムのY方向オフセット
                            val offsetY = if (isDragging) dragOffsetY else 0f

                            // ドラッグ中のスケールアニメーション
                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 1.05f else 1f,
                                animationSpec = spring(),
                                label = "scale"
                            )

                            // ドラッグ中のエレベーション
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 12.dp else 0.dp,
                                animationSpec = spring(),
                                label = "elevation"
                            )

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer { translationY = offsetY }
                                    .scale(scale)
                                    .animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null
                                    )
                            ) {
                                DraggableExerciseCard(
                                    exercise = exercise,
                                    colorType = colorType,
                                    isDragging = isDragging,
                                    elevation = elevation,
                                    context = context,
                                    onClick = {
                                        if (draggedItemId == null) onExerciseSelect(exercise)
                                    },
                                    onEdit = {
                                        if (draggedItemId == null) onRenameExercise(exercise)
                                    },
                                    onDelete = {
                                        if (draggedItemId == null) exerciseToDelete = exercise
                                    },
                                    onDragStart = {
                                        // ★IDで固定
                                        draggedItemId = exercise.id
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { deltaY ->
                                        val currentId = draggedItemId ?: return@DraggableExerciseCard
                                        dragOffsetY += deltaY

                                        // 現在のindexは毎回リストから引く（順番が変わってもズレない）
                                        val currentIndex = reorderedExercises.indexOfFirst { it.id == currentId }
                                        if (currentIndex == -1) return@DraggableExerciseCard

                                        // アイテムの高さをピクセルで計算
                                        val itemHeightPx = with(density) { totalItemHeightDp.toPx() }
                                        val movedPositions = (dragOffsetY / itemHeightPx).toInt()

                                        if (movedPositions != 0) {
                                            val targetIndex = (currentIndex + movedPositions)
                                                .coerceIn(0, reorderedExercises.size - 1)

                                            if (targetIndex != currentIndex) {
                                                val mutableList = reorderedExercises.toMutableList()
                                                val item = mutableList.removeAt(currentIndex)
                                                mutableList.add(targetIndex, item)
                                                reorderedExercises = mutableList

                                                // 移動分のオフセットを調整
                                                dragOffsetY -= movedPositions * itemHeightPx
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemId = null
                                        dragOffsetY = 0f
                                        onReorderExercises(reorderedExercises)
                                    },
                                    onDragCancel = {
                                        draggedItemId = null
                                        dragOffsetY = 0f
                                    },
                                    itemHeight = itemHeightDp
                                )
                            }
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

/**
 * ドラッグ可能な種目カード（カード全体を長押しでドラッグ）
 *
 * ★重要：pointerInput 内で使うコールバックは rememberUpdatedState で常に最新を参照する
 * （そうしないと並び替え後に「古いラムダ」を呼んでズレることがある）
 */
@Composable
private fun DraggableExerciseCard(
    exercise: ExerciseEntity,
    colorType: ExerciseColorType,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    context: Context,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    itemHeight: androidx.compose.ui.unit.Dp
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

    // ★pointerInput内で最新コールバックを使うためのラップ
    val onDragStartState by rememberUpdatedState(onDragStart)
    val onDragState by rememberUpdatedState(onDrag)
    val onDragEndState by rememberUpdatedState(onDragEnd)
    val onDragCancelState by rememberUpdatedState(onDragCancel)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .shadow(elevation, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            // keyはidでOK。中のコールバックは rememberUpdatedState で最新化する
            .pointerInput(exercise.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStartState() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragState(dragAmount.y)
                    },
                    onDragEnd = { onDragEndState() },
                    onDragCancel = { onDragCancelState() }
                )
            }
            .clickable(enabled = !isDragging) { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 種目名
        Text(
            text = exercise.getDisplayName(context),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        // 編集アイコン（青色）
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp),
            enabled = !isDragging
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit),
                tint = EditIconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // 削除アイコン（赤色）
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp),
            enabled = !isDragging
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = WorkoutColors.PureRed,
                modifier = Modifier.size(20.dp)
            )
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
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
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
            TextButton(
                onClick = { if (newName.isNotBlank()) onConfirm(newName.trim()) },
                enabled = newName.isNotBlank()
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
fun DeleteConfirmDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(stringResource(R.string.delete_confirm_message, itemName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = WorkoutColors.PureRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
