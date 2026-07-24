package com.poweder.simpleworkoutlog.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// 既存購入者の購入履歴照合に使用するため、商品IDは変更・削除しない。
private const val PRODUCT_ID_REMOVE_ADS = "remove_ads"
private const val TAG = "BillingManager"

/**
 * 既存の広告削除購入者の権利を維持するための購入履歴確認マネージャー。
 * 新規購入導線と商品カタログ取得は廃止し、復元処理のみを残す。
 */
class BillingManager(
    context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isConnecting = AtomicBoolean(false)

    @Volatile
    private var isDestroyed = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach(::handlePurchase)
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                restorePurchases()
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> logBillingError("purchase update", billingResult)
        }
    }

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        connectAndRestore()
    }

    private fun connectAndRestore() {
        if (isDestroyed || billingClient.isReady) return
        if (!isConnecting.compareAndSet(false, true)) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting.set(false)
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    restorePurchases()
                } else {
                    logBillingError("setup", billingResult)
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting.set(false)
                // enableAutoServiceReconnection() に再接続を任せる。
            }
        })
    }

    /** 機種変更・再インストール後、およびアプリ復帰時の購入履歴確認。 */
    fun restorePurchases() {
        if (isDestroyed) return
        if (!billingClient.isReady) {
            connectAndRestore()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val ownedRemoveAdsPurchases = purchases.filter { purchase ->
                    purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                ownedRemoveAdsPurchases.forEach(::handlePurchase)

                // trueだけでなくfalseも保存し、返金・取消時の状態を同期する。
                scope.launch {
                    settingsDataStore.setAdRemoved(ownedRemoveAdsPurchases.isNotEmpty())
                }
            } else {
                logBillingError("restore", billingResult)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRODUCT_ID_REMOVE_ADS)) return

        // 購入済みであることを確認できた時点で既存利用権を反映する。
        scope.launch {
            settingsDataStore.setAdRemoved(true)
        }

        if (purchase.isAcknowledged) return

        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                logBillingError("acknowledge", billingResult)
            }
        }
    }

    private fun logBillingError(operation: String, result: BillingResult) {
        Log.w(TAG, "$operation failed: ${result.responseCode} ${result.debugMessage}")
    }

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        isConnecting.set(false)
        billingClient.endConnection()
        scope.cancel()
    }
}
