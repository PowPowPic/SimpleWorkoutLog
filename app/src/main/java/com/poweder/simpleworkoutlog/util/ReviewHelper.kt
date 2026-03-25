package com.poweder.simpleworkoutlog.util

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.concurrent.TimeUnit

object ReviewHelper {

    private const val PREFS_NAME       = "review_prefs"
    private const val KEY_FIRST_LAUNCH = "first_launch_date"  // 初回起動日時（ms）
    private const val KEY_LAUNCH_COUNT = "launch_count"       // 起動回数
    private const val KEY_REVIEW_DONE  = "review_requested"   // API呼び出し済みフラグ

    // ★ 条件設定
    private val MIN_DAYS        = TimeUnit.DAYS.toMillis(30)  // インストールから30日以上
    private const val MIN_LAUNCHES = 10                       // 起動回数10回以上

    /**
     * 起動時に呼ぶ。条件を満たしていればIn-App Review APIを呼び出す。
     * MainActivity の onCreate から呼ぶこと。
     */
    fun checkAndRequest(activity: AppCompatActivity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // ★ 既にAPI呼び出し済みなら何もしない（= 実質1回だけ）
        if (prefs.getBoolean(KEY_REVIEW_DONE, false)) return

        // ★ 初回起動日時を記録（2回目以降は上書きしない）
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }

        // ★ 起動回数をカウントアップ
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount).apply()

        // ★ 経過日数チェック
        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, System.currentTimeMillis())
        val elapsed = System.currentTimeMillis() - firstLaunch
        if (elapsed < MIN_DAYS) return

        // ★ 起動回数チェック
        if (launchCount < MIN_LAUNCHES) return

        // ★ 条件クリア → API呼び出し＋済みフラグを保存
        prefs.edit().putBoolean(KEY_REVIEW_DONE, true).apply()
        launchReviewFlow(activity)
    }

    private fun launchReviewFlow(activity: AppCompatActivity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
                // ★ 結果（評価した/しない）はAPIから返ってこない仕様のため、ここでは何もしない
            }
            // 失敗してもクラッシュしない。済みフラグは保存済みなので次回も呼ばれない
        }
    }
}
