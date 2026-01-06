package com.poweder.simpleworkoutlog.ui.interval

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.dialog.getDisplayName
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adRemoved by viewModel.adRemoved.collectAsState()
    val currentExercise by viewModel.currentExercise.collectAsState()
    
    var durationMinutes by remember { mutableStateOf("20") }
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
            text = currentExercise?.getDisplayName(context) ?: stringResource(R.string.workout_interval),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 仮UI：時間入力
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.interval_coming_soon),
                style = MaterialTheme.typography.titleMedium,
                color = WorkoutColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 時間入力欄（Intervalカラー使用）
            val cardGradient = Brush.horizontalGradient(
                colors = listOf(
                    WorkoutColors.IntervalCardStart,
                    WorkoutColors.IntervalCardEnd,
                    WorkoutColors.IntervalCardStart
                )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardGradient)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { 
                        val filtered = it.filter { c -> c.isDigit() }
                        durationMinutes = filtered
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.duration_minutes)) },
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
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 下部ボタン：Home / Save
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
                        if (durationMinutes.isNotBlank() && durationMinutes != "0") {
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
            
            // Save ボタン
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WorkoutColors.AccentOrange)
                    .clickable {
                        // TODO: 保存処理
                        viewModel.clearSession()
                        onBack()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
            }
        }
        
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
