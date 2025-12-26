package io.horizontalsystems.subscriptions.core

import android.app.Activity
import android.content.Context

object UserSubscriptionManager {
    var authToken: String? = ""

    private val predefinedSubscriptions = listOf(
        Subscription(
            id = "premium",
            name = "Premium",
            description = "",
            actions = listOf(
                TokenInsights,
                AdvancedSearch,
                TradeSignals,
                RobberyProtection,
                SecureSend,
                ScamProtection,
                PrioritySupport,
                LossProtection,
            )
        ),
    )
    private lateinit var service: SubscriptionService

    val activeSubscriptionStateFlow
        get() = service.activeSubscriptionStateFlow

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

    suspend fun hasFreeTrial(): Boolean {
        val hasFreeTrial = getSubscriptions().flatMap { subscription ->
            getBasePlans(subscription.id)
        }.any { it.hasFreeTrial }
        return hasFreeTrial
    }

    suspend fun launchPurchaseFlow(
        subscriptionId: String,
        offerToken: String,
        activity: Activity
    ): HSPurchase? {
        return service.launchPurchaseFlow(subscriptionId, offerToken, activity)
    }

    fun launchManageSubscriptionScreen(context: Context) {
        service.launchManageSubscriptionScreen(context)
    }

    fun getBasePlans(subscriptionId: String): List<BasePlan> {
        return service.getBasePlans(subscriptionId)
    }

    suspend fun onResume() {
        service.onResume()
    }

    fun pause() {

    }

    suspend fun restore() {
        service.onResume()
    }
}
