package com.poweder.simpleworkoutlog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.poweder.simpleworkoutlog.MainActivity
import com.poweder.simpleworkoutlog.R
import com.poweder.simpleworkoutlog.domain.interval.IntervalPhase
import com.poweder.simpleworkoutlog.domain.interval.IntervalSnapshot

/**
 * インターバルタイマー通知ヘルパー
 * Foreground Service用の通知を作成・更新
 */
class IntervalNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "interval_timer_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    /**
     * 通知チャンネルを作成（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.interval_timer_channel_name),
                NotificationManager.IMPORTANCE_LOW  // 音を鳴らさない（Serviceで鳴らすため）
            ).apply {
                description = context.getString(R.string.interval_timer_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 通知を作成
     */
    fun createNotification(snapshot: IntervalSnapshot): Notification {
        // タップでアプリに戻る
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "interval")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = getPhaseTitle(snapshot.phase)
        val text = buildNotificationText(snapshot)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * 通知を更新
     */
    fun updateNotification(snapshot: IntervalSnapshot) {
        val notification = createNotification(snapshot)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 通知をキャンセル
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * フェーズに応じたタイトルを取得
     */
    private fun getPhaseTitle(phase: IntervalPhase): String {
        return when (phase) {
            IntervalPhase.WARMUP -> context.getString(R.string.phase_warmup)
            IntervalPhase.TRAINING -> context.getString(R.string.phase_training)
            IntervalPhase.REST -> context.getString(R.string.phase_rest)
            IntervalPhase.COOLDOWN -> context.getString(R.string.phase_cooldown)
            IntervalPhase.FINISHED -> context.getString(R.string.phase_finished)
            IntervalPhase.IDLE -> context.getString(R.string.interval_timer)
        }
    }

    /**
     * 通知テキストを構築（mm:ss Round x/y）
     */
    private fun buildNotificationText(snapshot: IntervalSnapshot): String {
        val timeStr = formatTime(snapshot.phaseRemainingSec)
        return if (snapshot.phase == IntervalPhase.TRAINING || snapshot.phase == IntervalPhase.REST) {
            "$timeStr   Round ${snapshot.roundIndex}/${snapshot.totalRounds}"
        } else {
            timeStr
        }
    }

    private fun formatTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }
}
