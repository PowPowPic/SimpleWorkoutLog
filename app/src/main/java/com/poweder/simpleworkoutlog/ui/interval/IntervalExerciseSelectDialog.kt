package com.poweder.simpleworkoutlog.ui.interval

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * インターバルトレーニング専用の種目選択ダイアログ
 * Tabata / HIIT の2択のみ、編集/削除/追加なし
 */
@Composable
fun IntervalExerciseSelectDialog(
    onHiitSelect: () -> Unit,
    onTabataSelect: () -> Unit,
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
                    text = stringResource(R.string.select_exercise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Tabata カード（先に表示）
                IntervalTypeCard(
                    text = "Tabata",
                    onClick = onTabataSelect
                )

                Spacer(modifier = Modifier.height(12.dp))

                // HIIT カード
                IntervalTypeCard(
                    text = "HIIT",
                    onClick = onHiitSelect
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel ボタンのみ
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

@Composable
private fun IntervalTypeCard(
    text: String,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.IntervalCardStart,
            WorkoutColors.IntervalCardEnd
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(gradient)
            .clickable { onClick() }
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}