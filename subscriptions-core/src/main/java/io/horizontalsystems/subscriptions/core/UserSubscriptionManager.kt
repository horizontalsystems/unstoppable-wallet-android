package io.horizontalsystems.subscriptions.core

import android.app.Activity

object UserSubscriptionManager {
    private lateinit var service: SubscriptionService

    fun registerService(service: SubscriptionService) {
        UserSubscriptionManager.service = service
    }

    fun isActionAllowed(paidAction: IPaidAction) : Boolean {
        return service.isActionAllowed(paidAction)
    }

    suspend fun getPlans(): List<SubscriptionPlan> {
        return service.getPlans()
    }

    fun launchPurchaseFlow(planId: String, activity: Activity) {
        service.launchPurchaseFlow(planId, activity)
    }
}
