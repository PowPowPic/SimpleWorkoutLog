package com.poweder.simpleworkoutlog.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 広告削除アイテムID（Google Play Console と一致させること）
private const val PRODUCT_ID_REMOVE_ADS = "remove_ads"

/**
 * Google Play 課金処理マネージャー
 * - 購入フローの起動
 * - 起動時の購入状態復元
 * - DataStore への購入フラグ保存
 */
class BillingManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 商品詳細（価格文字列取得用）
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails

    // エラーメッセージ通知用
    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                scope.launch { handlePurchase(purchase) }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // ユーザーキャンセルは無視
        } else {
            scope.launch { _errorEvent.emit(billingResult.debugMessage) }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connectAndInit()
    }

    /** BillingClient 接続 → 商品情報取得 → 既存購入チェック */
    private fun connectAndInit() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryProductDetails()
                        restorePurchases()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // ★ 切断時に自動再接続
                connectAndInit()
            }
        })
    }

    /** 商品情報（価格など）を取得 */
    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
        }
    }

    /** 機種変更・再インストール後の購入復元 */
    suspend fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasRemoveAds = result.purchasesList.any { purchase ->
                purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasRemoveAds) {
                settingsDataStore.setAdRemoved(true)
            }
        }
    }

    /** 購入フローを起動する */
    fun launchPurchaseFlow(activity: Activity) {
        // ★ 接続が切れている場合は再接続してから実行
        if (!billingClient.isReady) {
            connectAndInit()
            return
        }
        val details = _productDetails.value ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /** 購入完了処理（acknowledgment + DataStore 保存） */
    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRODUCT_ID_REMOVE_ADS)) return

        // 未承認の場合は承認
        if (!purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgeParams)
        }

        // DataStore に保存
        settingsDataStore.setAdRemoved(true)
    }

    fun clearError() {
        scope.launch { _errorEvent.emit(null) }
    }

    fun destroy() {
        billingClient.endConnection()
        scope.cancel()
    }
}