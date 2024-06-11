package io.horizontalsystems.subscriptions.core

import android.app.Activity

interface SubscriptionService {
    fun isActionAllowed(paidAction: IPaidAction): Boolean
    suspend fun getPlans(): List<SubscriptionPlan>
    fun launchPurchaseFlow(planId: String, activity: Activity)
}
