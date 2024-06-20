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

    suspend fun getSubscriptions(): List<Subscription> {
        return service.getSubscriptions()
    }

    suspend fun launchPurchaseFlow(subscriptionId: String, planId: String, activity: Activity): HSPurchase? {
        return service.launchPurchaseFlow(subscriptionId, planId, activity)
    }

    fun getBasePlans(subscriptionId: String): List<BasePlan> {
        return service.getBasePlans(subscriptionId)
    }
}
