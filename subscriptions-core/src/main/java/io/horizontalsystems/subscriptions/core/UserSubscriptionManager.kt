package io.horizontalsystems.subscriptions.core

import android.app.Activity

object UserSubscriptionManager {
    private val predefinedSubscriptions = listOf(
        Subscription(
            id = "test.subscription_1",
            name = "PRO",
            description = "",
            actions = listOf(
                TokenInsights,
                AdvancedSearch,
                TradeSignals,
                FavorableSwaps,
                TransactionSpeedTools,
                DuressMode,
                AddressVerification,
                Tor,
                PrivacyMode,
            )
        ),
        Subscription(
            id = "test.subscription_2",
            name = "VIP",
            description = "",
            actions = listOf(
                VIPSupport,
                VIPClub,
                TokenInsights,
                AdvancedSearch,
                TradeSignals,
                FavorableSwaps,
                TransactionSpeedTools,
                DuressMode,
                AddressVerification,
                Tor,
                PrivacyMode,
            )
        ),
    )
    private lateinit var service: SubscriptionService

    fun registerService(service: SubscriptionService) {
        service.predefinedSubscriptions = predefinedSubscriptions
        UserSubscriptionManager.service = service
    }

    fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return service.isActionAllowed(paidAction)
    }

    suspend fun getSubscriptions(): List<Subscription> {
        return service.getSubscriptions()
    }

    suspend fun launchPurchaseFlow(
        subscriptionId: String,
        planId: String,
        activity: Activity
    ): HSPurchase? {
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
