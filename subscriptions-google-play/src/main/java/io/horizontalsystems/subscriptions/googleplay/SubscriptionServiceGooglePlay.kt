package io.horizontalsystems.subscriptions.googleplay

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import io.horizontalsystems.subscriptionskit.IPaidAction
import io.horizontalsystems.subscriptionskit.SubscriptionPlan
import io.horizontalsystems.subscriptionskit.SubscriptionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubscriptionServiceGooglePlay(context: Context) : SubscriptionService {

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            // To be implemented in a later section.
        }

    private var billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Configure other settings.
        .build()

    private var productDetailsResult: ProductDetailsResult? = null

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    override fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return false
    }

    override suspend fun getPlans(): List<SubscriptionPlan> {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("product_id_example")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)

        // leverage queryProductDetails Kotlin extension function
        productDetailsResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params.build())
        }

        return listOf(
            SubscriptionPlan(
                id = "bronze",
                name = "Bronze",
            ),
            SubscriptionPlan(
                id = "silver",
                name = "Silver",
            ),
            SubscriptionPlan(
                id = "gold",
                name = "Gold",
            ),
        )
    }

    override fun launchPurchaseFlow(planId: String, activity: Activity) {
        val productDetails = productDetailsResult?.productDetailsList?.find {
            it.productId == planId
        }
        checkNotNull(productDetails)

        val selectedOfferToken = ""
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                .setProductDetails(productDetails)
                // For One-time product, "setOfferToken" method shouldn't be called.
                // For subscriptions, to get an offer token, call ProductDetails.subscriptionOfferDetails()
                // for a list of offers that are available to the user
                .setOfferToken(selectedOfferToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        // Launch the billing flow
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
    }
}
