package io.horizontalsystems.subscriptions.googleplay

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.HSPurchaseFailure
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.SubscriptionService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SubscriptionServiceGooglePlay(
    context: Context
) : SubscriptionService, PurchasesUpdatedListener {

    private var inProgressPurchaseResult: CancellableContinuation<HSPurchase?>? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Configure other settings.
        .build()

    private var productDetailsResult: ProductDetailsResult? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    coroutineScope.launch {
                        fetchAndHandleUserPurchases()
                    }
                    // The BillingClient is ready. You can query purchases here.
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    suspend fun onResume() {
        fetchAndHandleUserPurchases()
    }

    private suspend fun fetchAndHandleUserPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)

        val purchasesResult = billingClient.queryPurchasesAsync(params.build())

        purchasesResult.purchasesList.forEach { purchase ->
            handlePurchase(purchase)
        }
    }

    override fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return false
    }

    override fun getBasePlans(subscriptionId: String): List<BasePlan> {
        val productDetails = productDetailsResult?.productDetailsList?.firstOrNull {
            it.productId == subscriptionId
        }

        return buildList {
            val subscriptionOfferDetails = productDetails?.subscriptionOfferDetails
            subscriptionOfferDetails?.forEach { details ->
                val pricingPhases = buildList {
                    details.pricingPhases.pricingPhaseList.forEach {
                        add(
                            PricingPhase(
                                formattedPrice = it.formattedPrice,
                                billingPeriod = it.billingPeriod,
                            )
                        )
                    }
                }

                add(
                    BasePlan(
                        id = details.basePlanId,
                        pricingPhases = pricingPhases
                    )
                )
            }
        }
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("test.subscription_1")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("test.subscription_2")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        productDetailsResult = billingClient.queryProductDetails(params)

        return buildList {
            productDetailsResult?.productDetailsList?.forEach { productDetails ->
                add(
                    Subscription(
                        id = productDetails.productId,
                        name = productDetails.name,
                        description = productDetails.description,
                    )
                )
            }
        }
    }

    override suspend fun launchPurchaseFlow(subscriptionId: String, planId: String, activity: Activity): HSPurchase? {
        val productDetails = productDetailsResult?.productDetailsList?.find {
            it.productId == subscriptionId
        }
        checkNotNull(productDetails)

        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails

        val offerDetails = subscriptionOfferDetails?.firstOrNull {
            it.basePlanId == planId
        }

        checkNotNull(offerDetails)

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                .setProductDetails(productDetails)
                // For One-time product, "setOfferToken" method shouldn't be called.
                // For subscriptions, to get an offer token, call ProductDetails.subscriptionOfferDetails()
                // for a list of offers that are available to the user
                .setOfferToken(offerDetails.offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

        return suspendCancellableCoroutine {
            inProgressPurchaseResult = it
            it.invokeOnCancellation {
                inProgressPurchaseResult = null
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        inProgressPurchaseResult?.let {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    it.resume(HSPurchase(HSPurchase.Status.Purchased))
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    it.resume(null)
                }
                else -> {
                    it.resumeWithException(HSPurchaseFailure(billingResult.responseCode.toString(), billingResult.debugMessage))
                }
            }

            inProgressPurchaseResult = null
        }

        coroutineScope.launch {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else {
                // Handle any other error codes.
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.isAcknowledged) {
                addAcknowledgedPurchase(purchase)
            } else {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)

                val billingResult = billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    addAcknowledgedPurchase(purchase)
                }
            }
        }
    }

    private fun addAcknowledgedPurchase(purchase: Purchase) {
//        TODO("Not yet implemented")
    }
}
