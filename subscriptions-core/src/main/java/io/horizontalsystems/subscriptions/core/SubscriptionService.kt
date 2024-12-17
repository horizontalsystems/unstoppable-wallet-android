package io.horizontalsystems.subscriptions.core

import android.app.Activity

interface SubscriptionService {
    fun isActionAllowed(paidAction: IPaidAction): Boolean
    suspend fun getSubscriptions(): List<Subscription>
    suspend fun launchPurchaseFlow(subscriptionId: String, planId: String, activity: Activity): HSPurchase?
    fun getBasePlans(subscriptionId: String): List<BasePlan>
    suspend fun onResume()
}
