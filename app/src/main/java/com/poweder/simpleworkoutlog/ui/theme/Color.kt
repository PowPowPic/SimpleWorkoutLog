package com.poweder.simpleworkoutlog.ui.theme

import androidx.compose.ui.graphics.Color

object WorkoutColors {
    // 背景色（ダークグレイ → シルバー の強いグラデーション用）
    val BackgroundDark = Color(0xFF1A1A1A)      // 濃いダークグレー
    val BackgroundMedium = Color(0xFF8A8A8A)    // シルバーグレー
    val DialogBackground = Color(0xFF2D2D2D)    // ダイアログ背景
    
    // テキスト色
    val TextPrimary = Color(0xFFFFFFFF)         // 白
    val TextSecondary = Color(0xFFB0B0B0)       // グレー
    
    // アクセントカラー
    val AccentOrange = Color(0xFFFF9800)        // オレンジ
    val AccentOrangeLight = Color(0xFFFFB74D)   // 明るいオレンジ
    val PureRed = Color(0xFFE53935)             // 赤
    val PureBlue = Color(0xFF0000FF)            // 青（案内文用）
    val LinkBlue = Color(0xFF2196F3)            // リンク青
    
    // 筋トレカード（グレー系グラデーション）
    val StrengthCardStart = Color(0xFF616161)   // 濃いグレー
    val StrengthCardEnd = Color(0xFFBDBDBD)     // 明るいグレー
    
    // 有酸素カード（茶色系グラデーション）
    val CardioCardStart = Color(0xFF5D4037)     // 濃いブラウン
    val CardioCardEnd = Color(0xFFA1887F)       // 明るいブラウン
    
    // インターバルカード（オレンジ系グラデーション）
    val IntervalCardStart = Color(0xFF8D6E63)   // 濃いオレンジブラウン
    val IntervalCardEnd = Color(0xFFD7CCC8)     // 明るいベージュ
    
    // スタジオカード（紫系グラデーション）
    val StudioCardStart = Color(0xFF5E5370)     // 濃い紫グレー
    val StudioCardEnd = Color(0xFFB0A4C4)       // 明るいラベンダー
    
    // その他カード（青緑系グラデーション）
    val OtherCardStart = Color(0xFF00695C)      // 濃いティール
    val OtherCardEnd = Color(0xFF80CBC4)        // 明るいティール
    
    // メインカード（ダーク系グレー）
    val MainCardStart = Color(0xFF3D3D3D)       // 濃いグレー
    val MainCardEnd = Color(0xFF5A5A5A)         // 少し明るいグレー
    
    // Today Grand Total 用
    val GrandTotalBackground = Color(0xFF2D2D2D)
    val GrandTotalText = Color(0xFFFF9800)      // オレンジ
    
    // ボタン色
    val ButtonConfirm = Color(0xFF4CAF50)       // 緑
    val ButtonCancel = Color(0xFF757575)        // グレー
    
    // 空欄枠線
    val EmptySlotBorder = Color(0xFF555555)
    
    // ナビバー（透明）
    val NavBarBackground = Color.Transparent
    
    // ナビバーアイコン
    val NavBarIconSelected = Color(0xFFFF9800)  // オレンジ
    val NavBarIconUnselected = Color(0xFFB0B0B0) // グレー
}
