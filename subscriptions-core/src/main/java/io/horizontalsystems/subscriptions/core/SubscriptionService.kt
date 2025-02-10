package io.horizontalsystems.subscriptions.core

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionService {
    var predefinedSubscriptions: List<Subscription>
    val activeSubscriptionStateFlow: StateFlow<UserSubscription?>

    fun isActionAllowed(paidAction: IPaidAction): Boolean
    fun getActiveSubscriptions(): List<UserSubscription>
    suspend fun getSubscriptions(): List<Subscription>
    suspend fun launchPurchaseFlow(subscriptionId: String, offerToken: String, activity: Activity): HSPurchase?
    fun getBasePlans(subscriptionId: String): List<BasePlan>
    suspend fun onResume()
    fun launchManageSubscriptionScreen(context: Context) = Unit
}
