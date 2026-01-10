package com.poweder.simpleworkoutlog.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.ui.ads.TopBannerAd
import com.poweder.simpleworkoutlog.ui.theme.WorkoutColors
import com.poweder.simpleworkoutlog.ui.viewmodel.WorkoutViewModel
import com.poweder.simpleworkoutlog.util.DistanceUnit
import com.poweder.simpleworkoutlog.util.WeightUnit
import com.poweder.simpleworkoutlog.util.currentLogicalDate
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// タイマー設定用の定数（IntervalForegroundServiceと共有）
private const val PREFS_SETTINGS = "swl_prefs"
private const val KEY_TIMER_SOUND = "timer_sound_enabled"
private const val KEY_TIMER_VIBRATION = "timer_vibration_enabled"
private const val KEY_COUNTDOWN_LAST_5 = "countdown_last_5_seconds"

/**
 * ContextからActivityを取得する拡張関数
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val adRemoved by viewModel.adRemoved.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showWeightUnitDialog by remember { mutableStateOf(false) }
    var showDistanceUnitDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 言語変更確認用
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    var showLanguageConfirmDialog by remember { mutableStateOf(false) }
    var showApplyingDialog by remember { mutableStateOf(false) }

    // タイマー設定用SharedPreferences
    val timerSettingsPrefs = remember {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
    }

    // タイマー設定の状態
    var timerSoundEnabled by remember {
        mutableStateOf(timerSettingsPrefs.getBoolean(KEY_TIMER_SOUND, true))
    }
    var timerVibrationEnabled by remember {
        mutableStateOf(timerSettingsPrefs.getBoolean(KEY_TIMER_VIBRATION, true))
    }
    var countdownEnabled by remember {
        mutableStateOf(timerSettingsPrefs.getBoolean(KEY_COUNTDOWN_LAST_5, false))
    }

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

    val backgroundGradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.BackgroundDark,
            WorkoutColors.BackgroundMedium
        )
    )

    // 言語適用中ダイアログ
    if (showApplyingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = WorkoutColors.AccentOrange
                    )
                    Text(stringResource(R.string.applying_language))
                }
            },
            confirmButton = { }
        )

        // 言語適用処理
        LaunchedEffect(Unit) {
            delay(500)
            activity?.let { act ->
                viewModel.setLanguageAndRecreate(pendingLanguage, act)
            }
        }
    }

    // 言語変更確認ダイアログ
    if (showLanguageConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showLanguageConfirmDialog = false
                pendingLanguage = null
            },
            title = { Text(stringResource(R.string.language_confirm_title)) },
            text = { Text(stringResource(R.string.language_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLanguageConfirmDialog = false
                        showApplyingDialog = true
                    }
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLanguageConfirmDialog = false
                        pendingLanguage = null
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // ダイアログ
    if (showLanguageDialog) {
        LanguageSettingDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                showLanguageDialog = false
                pendingLanguage = language
                showLanguageConfirmDialog = true
            }
        )
    }

    if (showWeightUnitDialog) {
        WeightUnitDialog(
            currentUnit = weightUnit,
            onDismiss = { showWeightUnitDialog = false },
            onUnitSelected = { unit ->
                viewModel.setWeightUnit(unit)
                showWeightUnitDialog = false
            }
        )
    }

    if (showDistanceUnitDialog) {
        DistanceUnitDialog(
            currentUnit = distanceUnit,
            onDismiss = { showDistanceUnitDialog = false },
            onUnitSelected = { unit ->
                viewModel.setDistanceUnit(unit)
                showDistanceUnitDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteAllConfirmDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                viewModel.deleteAllData()
                showDeleteConfirmDialog = false
            }
        )
    }

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

        // ヘッダー
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = WorkoutColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 設定項目
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 言語設定
            SettingsItem(
                title = stringResource(R.string.settings_language_title),
                description = stringResource(R.string.settings_language_description),
                onClick = { showLanguageDialog = true }
            )

            // 重量単位
            SettingsItem(
                title = stringResource(R.string.settings_weight_unit_title),
                description = stringResource(R.string.settings_weight_unit_description, weightUnit.symbol),
                onClick = { showWeightUnitDialog = true }
            )

            // 距離単位
            SettingsItem(
                title = stringResource(R.string.settings_distance_unit_title),
                description = stringResource(R.string.settings_distance_unit_description, distanceUnit.symbol),
                onClick = { showDistanceUnitDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== タイマー設定セクション =====
            Text(
                text = stringResource(R.string.timer_settings_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = WorkoutColors.AccentOrange,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // タイマー音
            SettingsSwitchItem(
                title = stringResource(R.string.timer_sound),
                description = stringResource(R.string.timer_sound_desc),
                checked = timerSoundEnabled,
                onCheckedChange = { enabled ->
                    timerSoundEnabled = enabled
                    timerSettingsPrefs.edit().putBoolean(KEY_TIMER_SOUND, enabled).apply()
                }
            )

            // タイマーバイブ
            SettingsSwitchItem(
                title = stringResource(R.string.timer_vibration),
                description = stringResource(R.string.timer_vibration_desc),
                checked = timerVibrationEnabled,
                onCheckedChange = { enabled ->
                    timerVibrationEnabled = enabled
                    timerSettingsPrefs.edit().putBoolean(KEY_TIMER_VIBRATION, enabled).apply()
                }
            )

            // ラスト5秒カウントダウン
            SettingsSwitchItem(
                title = stringResource(R.string.countdown_last_5_seconds),
                description = stringResource(R.string.countdown_last_5_seconds_desc),
                checked = countdownEnabled,
                onCheckedChange = { enabled ->
                    countdownEnabled = enabled
                    timerSettingsPrefs.edit().putBoolean(KEY_COUNTDOWN_LAST_5, enabled).apply()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 全データ削除
            SettingsItem(
                title = stringResource(R.string.settings_delete_title),
                description = stringResource(R.string.settings_delete_description),
                onClick = { showDeleteConfirmDialog = true },
                isDanger = true
            )
        }

        // 下部ボタン：Home
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(WorkoutColors.ButtonCancel)
                .clickable { onBack() }
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

    }
}

@Composable
private fun SettingsItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val cardGradient = Brush.horizontalGradient(
        colors = if (isDanger) {
            listOf(
                WorkoutColors.PureRed.copy(alpha = 0.3f),
                WorkoutColors.PureRed.copy(alpha = 0.2f)
            )
        } else {
            listOf(
                WorkoutColors.StrengthCardStart,
                WorkoutColors.StrengthCardEnd
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardGradient)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDanger) WorkoutColors.PureRed else WorkoutColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = WorkoutColors.TextSecondary
            )
        }
    }
}

/**
 * スイッチ付き設定項目
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            WorkoutColors.StrengthCardStart,
            WorkoutColors.StrengthCardEnd
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardGradient)
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkoutColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WorkoutColors.AccentOrange,
                    checkedTrackColor = WorkoutColors.AccentOrange.copy(alpha = 0.5f),
                    uncheckedThumbColor = WorkoutColors.TextSecondary,
                    uncheckedTrackColor = WorkoutColors.TextSecondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

/**
 * 言語設定ダイアログ（○印チェック付き）
 */
@Composable
private fun LanguageSettingDialog(
    currentLanguage: String?,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit
) {
    val languages = listOf(
        null to stringResource(R.string.language_system_default),
        "en" to "English",
        "ja" to "日本語",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "it" to "Italiano",
        "ko" to "한국어",
        "ar" to "العربية",
        "th" to "ไทย",
        "tr" to "Türkçe",
        "vi" to "Tiếng Việt",
        "in" to "Bahasa Indonesia",
        "pt-BR" to "Português (Brasil)",
        "zh-TW" to "繁體中文"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                languages.forEach { (code, name) ->
                    val isSelected = code == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onLanguageSelected(code) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WorkoutColors.AccentOrange
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun WeightUnitDialog(
    currentUnit: WeightUnit,
    onDismiss: () -> Unit,
    onUnitSelected: (WeightUnit) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_weight_unit_title)) },
        text = {
            Column {
                WeightUnit.values().forEach { unit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnitSelected(unit) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = unit == currentUnit,
                            onClick = { onUnitSelected(unit) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WorkoutColors.AccentOrange
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${unit.displayName} (${unit.symbol})")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun DistanceUnitDialog(
    currentUnit: DistanceUnit,
    onDismiss: () -> Unit,
    onUnitSelected: (DistanceUnit) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_distance_unit_title)) },
        text = {
            Column {
                DistanceUnit.values().forEach { unit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnitSelected(unit) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = unit == currentUnit,
                            onClick = { onUnitSelected(unit) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = WorkoutColors.AccentOrange
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${unit.displayName} (${unit.symbol})")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun DeleteAllConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_all_title)) },
        text = { Text(stringResource(R.string.delete_all_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.common_ok),
                    color = WorkoutColors.PureRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}