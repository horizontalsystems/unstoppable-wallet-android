package io.horizontalsystems.subscriptions.dev

import android.app.Activity
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.SubscriptionService

class SubscriptionServiceDev : SubscriptionService {

    override var predefinedSubscriptions: List<Subscription> = listOf()

    override suspend fun onResume() = Unit

    override fun isActionAllowed(paidAction: IPaidAction) = true

    override fun getBasePlans(subscriptionId: String): List<BasePlan> = listOf()

    override suspend fun getSubscriptions(): List<Subscription> = listOf()

    override suspend fun launchPurchaseFlow(
        subscriptionId: String,
        planId: String,
        activity: Activity,
    ) = null
}
