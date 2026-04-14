package com.quantum.wallet.subscriptions.fdroid

import android.app.Activity
import android.content.Context
import com.quantum.wallet.subscriptions.core.BasePlan
import com.quantum.wallet.subscriptions.core.HSPurchase
import com.quantum.wallet.subscriptions.core.IPaidAction
import com.quantum.wallet.subscriptions.core.Subscription
import com.quantum.wallet.subscriptions.core.SubscriptionService
import com.quantum.wallet.subscriptions.core.UserSubscription
import com.quantum.wallet.subscriptions.core.PrioritySupport
import kotlinx.coroutines.flow.MutableStateFlow

class SubscriptionServiceFDroid(context: Context) : SubscriptionService {
    override var predefinedSubscriptions = listOf<Subscription>()
    override val activeSubscriptionStateFlow = MutableStateFlow(null)

    override fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return paidAction !is PrioritySupport
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