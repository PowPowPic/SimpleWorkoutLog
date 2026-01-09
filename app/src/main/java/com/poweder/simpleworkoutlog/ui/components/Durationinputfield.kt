package com.poweder.simpleworkoutlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors

/**
 * 時間入力コンポーネント（時間・分・秒の3フィールド）
 * シンプルシリーズ共通の時間入力UI
 *
 * @param hours 時間の値
 * @param minutes 分の値
 * @param seconds 秒の値
 * @param onHoursChange 時間変更コールバック
 * @param onMinutesChange 分変更コールバック
 * @param onSecondsChange 秒変更コールバック
 * @param modifier Modifier
 */
@Composable
fun DurationInputField(
    hours: String,
    minutes: String,
    seconds: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // ラベル
        Text(
            text = stringResource(R.string.duration_minutes),
            style = MaterialTheme.typography.bodySmall,
            color = WorkoutColors.AccentOrange,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 時間
            OutlinedTextField(
                value = hours,
                onValueChange = { newValue ->
                    // 数字のみ、最大2桁
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    onHoursChange(filtered)
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.duration_hours_placeholder),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary,
                    focusedBorderColor = WorkoutColors.AccentOrange,
                    unfocusedBorderColor = WorkoutColors.TextSecondary
                ),
                modifier = Modifier.weight(1f)
            )

            // コロン
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = WorkoutColors.TextPrimary
            )

            // 分
            OutlinedTextField(
                value = minutes,
                onValueChange = { newValue ->
                    // 数字のみ、最大2桁
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    onMinutesChange(filtered)
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.duration_minutes_placeholder),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary,
                    focusedBorderColor = WorkoutColors.AccentOrange,
                    unfocusedBorderColor = WorkoutColors.TextSecondary
                ),
                modifier = Modifier.weight(1f)
            )

            // コロン
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = WorkoutColors.TextPrimary
            )

            // 秒
            OutlinedTextField(
                value = seconds,
                onValueChange = { newValue ->
                    // 数字のみ、最大2桁
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    onSecondsChange(filtered)
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.duration_seconds_placeholder),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WorkoutColors.TextPrimary,
                    unfocusedTextColor = WorkoutColors.TextPrimary,
                    focusedBorderColor = WorkoutColors.AccentOrange,
                    unfocusedBorderColor = WorkoutColors.TextSecondary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 時間・分・秒を秒数に変換
 */
fun durationToSeconds(hours: String, minutes: String, seconds: String): Int {
    val h = hours.toIntOrNull() ?: 0
    val m = minutes.toIntOrNull() ?: 0
    val s = seconds.toIntOrNull() ?: 0
    return h * 3600 + m * 60 + s
}

/**
 * 秒数を時間・分・秒に分解
 */
fun secondsToDuration(totalSeconds: Int): Triple<String, String, String> {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return Triple(
        if (h > 0) h.toString() else "",
        if (h > 0 || m > 0) m.toString() else "",
        if (h > 0 || m > 0 || s > 0) s.toString() else ""
    )
}