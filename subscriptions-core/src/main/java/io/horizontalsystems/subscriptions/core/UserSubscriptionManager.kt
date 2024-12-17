package io.horizontalsystems.subscriptions.core

import android.app.Activity

object UserSubscriptionManager {
    private val predefinedSubscriptions = listOf(
        Subscription(
            id = "test.subscription_1",
            name = "Test Subscription #1",
            description = "",
            actions = listOf(EnableWatchlistSignals)
        ),
        Subscription(
            id = "test.subscription_2",
            name = "Test Subscription #2",
            description = "",
            actions = listOf()
        ),
    )
    private lateinit var service: SubscriptionService

    fun registerService(service: SubscriptionService) {
        service.predefinedSubscriptions = predefinedSubscriptions
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

    suspend fun onResume() {
        service.onResume()
    }

    fun pause() {

    }
}
