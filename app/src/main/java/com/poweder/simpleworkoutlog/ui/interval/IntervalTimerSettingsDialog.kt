package com.poweder.simpleworkoutlog.ui.interval

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * インターバルタイマー設定ダイアログ
 */
@Composable
fun IntervalTimerSettingsDialog(
    initialSettings: IntervalTimerSettings,
    onConfirm: (IntervalTimerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var warmupSeconds by remember { mutableStateOf(initialSettings.warmupSeconds.toString()) }
    var trainingSeconds by remember { mutableStateOf(initialSettings.trainingSeconds.toString()) }
    var restSeconds by remember { mutableStateOf(initialSettings.restSeconds.toString()) }
    var sets by remember { mutableStateOf(initialSettings.sets.toString()) }
    var cooldownSeconds by remember { mutableStateOf(initialSettings.cooldownSeconds.toString()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = WorkoutColors.DialogBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.interval_timer_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // プリセットボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            val tabata = IntervalTimerSettings.tabataDefault()
                            warmupSeconds = tabata.warmupSeconds.toString()
                            trainingSeconds = tabata.trainingSeconds.toString()
                            restSeconds = tabata.restSeconds.toString()
                            sets = tabata.sets.toString()
                            cooldownSeconds = tabata.cooldownSeconds.toString()
                        }
                    ) {
                        Text(stringResource(R.string.preset_tabata))
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val hiit = IntervalTimerSettings.hiitDefault()
                            warmupSeconds = hiit.warmupSeconds.toString()
                            trainingSeconds = hiit.trainingSeconds.toString()
                            restSeconds = hiit.restSeconds.toString()
                            sets = hiit.sets.toString()
                            cooldownSeconds = hiit.cooldownSeconds.toString()
                        }
                    ) {
                        Text(stringResource(R.string.preset_hiit))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // ウォームアップ時間
                OutlinedTextField(
                    value = warmupSeconds,
                    onValueChange = { warmupSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.warmup_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // トレーニング時間
                OutlinedTextField(
                    value = trainingSeconds,
                    onValueChange = { trainingSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.training_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // インターバル時間
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.rest_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // セット数
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.number_of_sets)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // クールダウン時間
                OutlinedTextField(
                    value = cooldownSeconds,
                    onValueChange = { cooldownSeconds = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.cooldown_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WorkoutColors.TextPrimary,
                        unfocusedTextColor = WorkoutColors.TextPrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 総時間表示
                val totalSeconds = calculateTotalSeconds(
                    warmupSeconds.toIntOrNull() ?: 0,
                    trainingSeconds.toIntOrNull() ?: 0,
                    restSeconds.toIntOrNull() ?: 0,
                    sets.toIntOrNull() ?: 0,
                    cooldownSeconds.toIntOrNull() ?: 0
                )
                Text(
                    text = stringResource(R.string.total_time_format, formatSeconds(totalSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    
                    Button(
                        onClick = {
                            val settings = IntervalTimerSettings(
                                warmupSeconds = warmupSeconds.toIntOrNull() ?: 0,
                                trainingSeconds = trainingSeconds.toIntOrNull() ?: 20,
                                restSeconds = restSeconds.toIntOrNull() ?: 10,
                                sets = sets.toIntOrNull() ?: 1,
                                cooldownSeconds = cooldownSeconds.toIntOrNull() ?: 0
                            )
                            onConfirm(settings)
                        },
                        enabled = (trainingSeconds.toIntOrNull() ?: 0) > 0 && (sets.toIntOrNull() ?: 0) > 0
                    ) {
                        Text(stringResource(R.string.start_timer))
                    }
                }
            }
        }
    }
}

private fun calculateTotalSeconds(
    warmup: Int,
    training: Int,
    rest: Int,
    sets: Int,
    cooldown: Int
): Int {
    if (sets <= 0 || training <= 0) return 0
    var total = warmup
    total += training * sets
    total += rest * (sets - 1).coerceAtLeast(0)
    total += cooldown
    return total
}

private fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "${minutes}:${secs.toString().padStart(2, '0')}"
    } else {
        "${secs}s"
    }
}
