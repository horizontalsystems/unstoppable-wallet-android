package io.horizontalsystems.subscriptions.core

import android.app.Activity

interface SubscriptionService {
    var predefinedSubscriptions: List<Subscription>

    fun isActionAllowed(paidAction: IPaidAction): Boolean
    fun getActiveSubscriptions(): List<Subscription>
    suspend fun getSubscriptions(): List<Subscription>
    suspend fun launchPurchaseFlow(subscriptionId: String, planId: String, activity: Activity): HSPurchase?
    fun getBasePlans(subscriptionId: String): List<BasePlan>
    suspend fun onResume()
}
