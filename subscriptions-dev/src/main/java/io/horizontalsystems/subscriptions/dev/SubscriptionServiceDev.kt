package io.horizontalsystems.subscriptions.dev

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.SubscriptionService

class SubscriptionServiceDev(context: Context) : SubscriptionService {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override var predefinedSubscriptions: List<Subscription> = listOf()

    override suspend fun onResume() = Unit

    override fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return prefs.getString(KEY_ACTIVE_SUBSCRIPTION, null) != null
    }

    private fun setActiveSubscription(subscription: String?) {
        prefs.edit().putString(KEY_ACTIVE_SUBSCRIPTION, subscription).commit()
    }

    override fun getBasePlans(subscriptionId: String): List<BasePlan> =
        if (subscriptionId == "test.subscription_1") {
            //PRO plans
            listOf(
                BasePlan(
                    id = "plan-1",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$10.00",
                            billingPeriod = "P1M"
                        )
                    )
                ),
                BasePlan(
                    id = "plan-2",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$100.00",
                            billingPeriod = "P1Y"
                        )
                    )
                ),
            )
        } else {
            //VIP plans
            listOf(
                BasePlan(
                    id = "plan-1",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$15.00",
                            billingPeriod = "P1M"
                        )
                    )
                ),
                BasePlan(
                    id = "plan-2",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$150.00",
                            billingPeriod = "P1Y"
                        )
                    )
                ),
            )
        }

    override suspend fun getSubscriptions(): List<Subscription> = predefinedSubscriptions

    override fun getActiveSubscriptions(): List<Subscription> {
        val activeSubscription = prefs.getString(KEY_ACTIVE_SUBSCRIPTION, null)
        return predefinedSubscriptions.filter { it.id == activeSubscription }
    }

    override suspend fun launchPurchaseFlow(
        subscriptionId: String,
        planId: String,
        activity: Activity,
    ): HSPurchase? {
        //toggle actionsAllowed value
        val activeSubscription = prefs.getString(KEY_ACTIVE_SUBSCRIPTION, null)
        if(activeSubscription == null) {
            setActiveSubscription(subscriptionId)
            return HSPurchase(HSPurchase.Status.Purchased)
        } else {
            setActiveSubscription(null)
            return null
        }
    }

    companion object {
        private const val PREFS_NAME = "subscription_service_dev_prefs"
        private const val KEY_ACTIVE_SUBSCRIPTION = "active_subscription"
    }
}
