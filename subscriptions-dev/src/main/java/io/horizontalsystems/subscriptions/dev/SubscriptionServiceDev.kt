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
        return prefs.getBoolean(KEY_ACTIONS_ALLOWED, false)
    }

    private fun setActionsAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ACTIONS_ALLOWED, allowed).commit()
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

    override suspend fun launchPurchaseFlow(
        subscriptionId: String,
        planId: String,
        activity: Activity,
    ): HSPurchase? {
        //toggle actionsAllowed value
        val actionsAllowed = prefs.getBoolean(KEY_ACTIONS_ALLOWED, false)
        setActionsAllowed(!actionsAllowed)

        if (actionsAllowed) {
            return null
        } else {
            return HSPurchase(HSPurchase.Status.Purchased)
        }
    }

    companion object {
        private const val PREFS_NAME = "subscription_service_dev_prefs"
        private const val KEY_ACTIONS_ALLOWED = "actions_allowed"
    }
}
