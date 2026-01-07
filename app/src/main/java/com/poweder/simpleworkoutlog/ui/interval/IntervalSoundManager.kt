package com.poweder.simpleworkoutlog.ui.interval

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * インターバルタイマー用サウンドマネージャー
 * ToneGeneratorのみを使用（res/raw不要で堅牢）
 * 残り5秒からの短音「ピッ」と、0秒での長音「ピー」を管理
 */
class IntervalSoundManager {

    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            // ToneGenerator初期化失敗時は無視（音なしで動作）
        }
    }

    /**
     * 短音を再生（残り5,4,3,2,1秒）- 150ms
     */
    fun playShortBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            // 無視
        }
    }

    /**
     * 長音を再生（残り0秒 = フェーズ終了）- 600ms
     */
    fun playLongBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        } catch (e: Exception) {
            // 無視
        }
    }

    /**
     * リソースを解放
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // 無視
        }
    }
}