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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * インターバルトレーニング専用の種目選択ダイアログ
 * TABATA / HIIT / EMOM の3択
 * 順序: TABATA → HIIT → EMOM（固定）
 */
@Composable
fun IntervalExerciseSelectDialog(
    onTabataSelect: () -> Unit,
    onHiitSelect: () -> Unit,
    onEmomSelect: () -> Unit,
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

                // 1. TABATA（最初）
                IntervalTypeCard(
                    title = "TABATA",
                    description = stringResource(R.string.preset_tabata_desc),
                    onClick = onTabataSelect
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. HIIT（2番目）
                IntervalTypeCard(
                    title = "HIIT",
                    description = stringResource(R.string.preset_hiit_desc),
                    onClick = onHiitSelect
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. EMOM（最後）
                IntervalTypeCard(
                    title = "EMOM",
                    description = stringResource(R.string.preset_emom_desc),
                    onClick = onEmomSelect
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel ボタン
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
    title: String,
    description: String,
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
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black
            )
        }
    }
}