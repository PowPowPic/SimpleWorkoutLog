package com.poweder.simpleworkoutlog.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SWL カラーパレット
 * - グレー系グラデーション背景（バーベル/プレートのメタル感）
 * - 筋トレ系：寒色寄りグレー
 * - 有酸素：暖色寄りグレー（薄め）
 * - インターバル：暖色寄りグレー（濃いめ）
 * - スタジオ：紫混じりのグレー
 * - アクセント：控えめなオレンジ
 */
object WorkoutColors {
    // 背景グラデーション（左→右：濃→薄）
    val BackgroundDark = Color(0xFF2D2D2D)      // ダークグレー
    val BackgroundMedium = Color(0xFF4A4A4A)    // ミディアムグレー
    
    // カード色 - 筋トレ系（寒色寄りグレー）
    val StrengthCardStart = Color(0xFF5A6066)   // 青みがかったグレー
    val StrengthCardEnd = Color(0xFF7A8288)     // 明るめの寒色グレー
    
    // カード色 - 有酸素系（暖色寄りグレー・薄め）
    val CardioCardStart = Color(0xFF6B5D55)     // オレンジ混じりのグレー
    val CardioCardEnd = Color(0xFF8A7A70)       // 明るめの暖色グレー
    
    // カード色 - インターバル系（暖色寄りグレー・濃いめ）
    val IntervalCardStart = Color(0xFF7A5A45)   // 濃いめのオレンジ混じりグレー
    val IntervalCardEnd = Color(0xFF9A7A65)     // 明るめの濃いウォームグレー
    
    // カード色 - スタジオ系（紫混じりのグレー）
    val StudioCardStart = Color(0xFF5D5570)     // 紫混じりのグレー
    val StudioCardEnd = Color(0xFF7D7590)       // 明るめの紫グレー
    
    // アクセントカラー（オレンジ系）
    val AccentOrange = Color(0xFFE07B3C)        // メインアクセント
    val AccentOrangeLight = Color(0xFFF5A060)   // ライトアクセント
    
    // テキストカラー
    val TextPrimary = Color(0xFFFFFFFF)         // 白（主要テキスト）
    val TextSecondary = Color(0xFFB0B0B0)       // グレー（補助テキスト）
    val TextOnLight = Color(0xFF2D2D2D)         // 明るい背景用
    
    // ダイアログ背景
    val DialogBackground = Color(0xFF3A3A3A)
    
    // ボタン
    val ButtonConfirm = AccentOrange
    val ButtonCancel = Color(0xFF666666)
    
    // 状態
    val Checked = Color(0xFF888888)
    val Unchecked = TextPrimary
    
    // 特殊カラー
    val PureBlue = Color(0xFF0000FF)            // 設定案内リンク
    val PureRed = Color(0xFFFF4444)             // 削除など
    val EmptySlotBorder = Color(0xFF606060)     // 空欄スロットの境界線
    
    // ナビバー
    val NavBarBackground = Color(0xFF1E1E1E)
    val NavBarSelected = AccentOrange
    val NavBarUnselected = Color(0xFF888888)
    
    // Today Grand Total
    val GrandTotalBackground = Color(0xFF3D3D3D)
    val GrandTotalText = AccentOrange
}
