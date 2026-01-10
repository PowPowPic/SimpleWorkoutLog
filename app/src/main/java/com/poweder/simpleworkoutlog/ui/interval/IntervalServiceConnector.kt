package com.poweder.simpleworkoutlog.ui.interval

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.poweder.simpleworkoutlog.domain.interval.IntervalPlan
import com.poweder.simpleworkoutlog.domain.interval.IntervalSnapshot
import com.poweder.simpleworkoutlog.service.IntervalForegroundService
import kotlinx.coroutines.flow.StateFlow

/**
 * IntervalForegroundService への接続を管理するヘルパー
 * 
 * UI側（ViewModel/Screen）からServiceを操作するためのインターフェース
 */
class IntervalServiceConnector(private val context: Context) {

    private var service: IntervalForegroundService? = null
    private var isBound = false

    /**
     * Service からの StateFlow（UI監視用）
     */
    val snapshotFlow: StateFlow<IntervalSnapshot?>?
        get() = service?.snapshotFlow

    /**
     * Service に接続中かどうか
     */
    val isConnected: Boolean
        get() = isBound && service != null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? IntervalForegroundService.LocalBinder
            service = localBinder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    /**
     * Service にバインド
     */
    fun bind() {
        if (!isBound) {
            val intent = Intent(context, IntervalForegroundService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Service からアンバインド
     */
    fun unbind() {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                // Ignore
            }
            isBound = false
            service = null
        }
    }

    /**
     * タイマーを開始
     */
    fun startTimer(plan: IntervalPlan) {
        val intent = Intent(context, IntervalForegroundService::class.java).apply {
            action = IntervalForegroundService.ACTION_START
            putExtra(IntervalForegroundService.EXTRA_PLAN_WARMUP, plan.warmupSec)
            putExtra(IntervalForegroundService.EXTRA_PLAN_TRAINING, plan.trainingSec)
            putExtra(IntervalForegroundService.EXTRA_PLAN_REST, plan.restSec)
            putExtra(IntervalForegroundService.EXTRA_PLAN_ROUNDS, plan.rounds)
            putExtra(IntervalForegroundService.EXTRA_PLAN_COOLDOWN, plan.cooldownSec)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // Bind after starting
        bind()
    }

    /**
     * タイマーを一時停止
     */
    fun pauseTimer() {
        val intent = Intent(context, IntervalForegroundService::class.java).apply {
            action = IntervalForegroundService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    /**
     * タイマーを再開
     */
    fun resumeTimer() {
        val intent = Intent(context, IntervalForegroundService::class.java).apply {
            action = IntervalForegroundService.ACTION_RESUME
        }
        context.startService(intent)
    }

    /**
     * タイマーを停止
     */
    fun stopTimer() {
        val intent = Intent(context, IntervalForegroundService::class.java).apply {
            action = IntervalForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
