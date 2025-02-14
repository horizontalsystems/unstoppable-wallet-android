package io.horizontalsystems.subscriptions.fdroid

import android.app.Activity
import android.content.Context
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.SubscriptionService
import io.horizontalsystems.subscriptions.core.UserSubscription
import io.horizontalsystems.subscriptions.core.VIPSupport
import kotlinx.coroutines.flow.MutableStateFlow

class SubscriptionServiceFDroid(context: Context) : SubscriptionService {
    override var predefinedSubscriptions = listOf<Subscription>()
    override val activeSubscriptionStateFlow = MutableStateFlow(null)

    override fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return paidAction !is VIPSupport
    }

    override fun getActiveSubscriptions(): List<UserSubscription> {
        return listOf()
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        return listOf()
    }

    override suspend fun launchPurchaseFlow(
        subscriptionId: String,
        offerToken: String,
        activity: Activity,
    ): HSPurchase? {
        return null
    }

    override fun getBasePlans(subscriptionId: String): List<BasePlan> {
        return listOf()
    }

    override suspend fun onResume() {

    }
}