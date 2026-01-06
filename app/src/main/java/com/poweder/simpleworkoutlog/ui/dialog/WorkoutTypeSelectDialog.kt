package com.poweder.simpleworkoutlog.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

@Composable
fun WorkoutTypeSelectDialog(
    onStrengthSelect: () -> Unit,
    onCardioSelect: () -> Unit,
    onIntervalSelect: () -> Unit,
    onStudioSelect: () -> Unit = {},
    onAddNewType: () -> Unit = {},
    onDismiss: () -> Unit
) {
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
                    text = stringResource(R.string.select_workout_type),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                WorkoutTypeCard(stringResource(R.string.workout_strength), WorkoutColorType.STRENGTH, onStrengthSelect)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutTypeCard(stringResource(R.string.workout_cardio), WorkoutColorType.CARDIO, onCardioSelect)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutTypeCard(stringResource(R.string.workout_interval), WorkoutColorType.INTERVAL, onIntervalSelect)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutTypeCard(stringResource(R.string.workout_studio), WorkoutColorType.STUDIO, onStudioSelect)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutTypeCard(stringResource(R.string.empty_slot), WorkoutColorType.EMPTY, onAddNewType)

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WorkoutColors.AccentOrange.copy(alpha = 0.2f))
                        .clickable { onAddNewType() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.add_workout_type),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutColors.AccentOrange
                    )
                }
            }
        }
    }
}

private enum class WorkoutColorType {
    STRENGTH, CARDIO, INTERVAL, STUDIO, EMPTY
}

@Composable
private fun WorkoutTypeCard(text: String, colorType: WorkoutColorType, onClick: () -> Unit) {
    val gradient = when (colorType) {
        WorkoutColorType.STRENGTH -> Brush.horizontalGradient(listOf(WorkoutColors.StrengthCardStart, WorkoutColors.StrengthCardEnd, WorkoutColors.StrengthCardStart))
        WorkoutColorType.CARDIO -> Brush.horizontalGradient(listOf(WorkoutColors.CardioCardStart, WorkoutColors.CardioCardEnd, WorkoutColors.CardioCardStart))
        WorkoutColorType.INTERVAL -> Brush.horizontalGradient(listOf(WorkoutColors.IntervalCardStart, WorkoutColors.IntervalCardEnd, WorkoutColors.IntervalCardStart))
        WorkoutColorType.STUDIO -> Brush.horizontalGradient(listOf(WorkoutColors.StudioCardStart, WorkoutColors.StudioCardEnd, WorkoutColors.StudioCardStart))
        WorkoutColorType.EMPTY -> Brush.horizontalGradient(listOf(WorkoutColors.BackgroundMedium, WorkoutColors.BackgroundDark, WorkoutColors.BackgroundMedium))
    }

    val borderModifier = if (colorType == WorkoutColorType.EMPTY) {
        Modifier.border(1.dp, WorkoutColors.EmptySlotBorder, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(borderModifier)
            .background(gradient)
            .clickable { onClick() }
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (colorType == WorkoutColorType.EMPTY) FontWeight.Normal else FontWeight.Bold,
            color = if (colorType == WorkoutColorType.EMPTY) WorkoutColors.TextSecondary else WorkoutColors.TextPrimary
        )
    }
}