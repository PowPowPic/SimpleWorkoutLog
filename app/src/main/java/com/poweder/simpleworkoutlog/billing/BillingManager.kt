package com.poweder.simpleworkoutlog.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.poweder.simpleworkoutlog.data.preferences.SettingsDataStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 広告削除アイテムID（Google Play Console と一致させること）
private const val PRODUCT_ID_REMOVE_ADS = "remove_ads"

/**
 * Google Play 課金処理マネージャー
 * - Google Play Billing Library 9 対応
 * - 購入フローの起動
 * - 起動時・復帰時の購入状態復元
 * - DataStore への購入フラグ保存
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

    // 商品詳細（価格文字列取得用）
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails

    // エラーメッセージ通知用
    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach(::handlePurchase)
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // ユーザーキャンセルはエラー表示しない
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Play側の所有情報とDataStoreを再同期する
                restorePurchases()
            }

            else -> emitError(billingResult.debugMessage)
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
        connectAndInit()
    }

    /** BillingClient 接続 → 商品情報取得 → 既存購入チェック */
    private fun connectAndInit() {
        if (isDestroyed || billingClient.isReady) return
        if (!isConnecting.compareAndSet(false, true)) return

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting.set(false)

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    restorePurchases()
                } else {
                    emitError(billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting.set(false)
                // enableAutoServiceReconnection() に再接続を任せる。
                // 次の課金API呼び出し時にも必要に応じて自動再接続される。
            }
        })
    }

    /** 商品情報（価格など）を取得 */
    private fun queryProductDetails() {
        if (isDestroyed) return
        if (!billingClient.isReady) {
            connectAndInit()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = queryResult.productDetailsList
                    .firstOrNull { it.productId == PRODUCT_ID_REMOVE_ADS }

                if (_productDetails.value == null && queryResult.unfetchedProductList.isNotEmpty()) {
                    val unfetched = queryResult.unfetchedProductList.first()
                    emitError(
                        "商品情報を取得できませんでした " +
                            "(productId=${unfetched.productId}, statusCode=${unfetched.statusCode})"
                    )
                }
            } else {
                _productDetails.value = null
                emitError(billingResult.debugMessage)
            }
        }
    }

    /** 機種変更・再インストール後、およびアプリ復帰時の購入復元 */
    fun restorePurchases() {
        if (isDestroyed) return
        if (!billingClient.isReady) {
            connectAndInit()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasRemoveAds = purchases.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                // trueだけでなくfalseも保存し、返金・取消時の状態を同期する。
                scope.launch {
                    settingsDataStore.setAdRemoved(hasRemoveAds)
                }
            } else {
                emitError(billingResult.debugMessage)
            }
        }
    }

    /** 購入フローを起動する */
    fun launchPurchaseFlow(activity: Activity) {
        if (isDestroyed) return
        if (!billingClient.isReady) {
            connectAndInit()
            return
        }

        val details = _productDetails.value
        if (details == null) {
            queryProductDetails()
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        // PBL 9の1回限りの商品で利用可能なオファートークンがある場合は渡す。
        val offerToken = details.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.offerToken
            ?: details.oneTimePurchaseOfferDetails?.offerToken

        if (!offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(offerToken)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                restorePurchases()
            } else {
                emitError(result.debugMessage)
            }
        }
    }

    /** 購入完了処理（acknowledgment + DataStore 保存） */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PRODUCT_ID_REMOVE_ADS)) return

        // 購入済みであることを確認できた時点で利用権を反映する。
        scope.launch {
            settingsDataStore.setAdRemoved(true)
        }

        if (purchase.isAcknowledged) return

        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                emitError(billingResult.debugMessage)
            }
        }
    }

    private fun emitError(message: String?) {
        val safeMessage = message?.takeIf { it.isNotBlank() } ?: "Google Play Billing error"
        scope.launch {
            _errorEvent.emit(safeMessage)
        }
    }

    fun clearError() {
        scope.launch {
            _errorEvent.emit(null)
        }
    }

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        isConnecting.set(false)
        billingClient.endConnection()
        scope.cancel()
    }
}
