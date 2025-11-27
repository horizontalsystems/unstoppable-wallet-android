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
import io.horizontalsystems.subscriptions.core.UserSubscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class SubscriptionServiceDev(private val context: Context) : SubscriptionService {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override var predefinedSubscriptions: List<Subscription> = listOf()
        set(value) {
            field = value

            activeSubscriptionStateFlow.update {
                getActiveSubscriptions().firstOrNull()
            }
        }
    override val activeSubscriptionStateFlow = MutableStateFlow(getActiveSubscriptions().firstOrNull())

    override fun launchManageSubscriptionScreen(context: Context) {
        setActiveSubscription(null)
    }

    override suspend fun onResume() = Unit

    override fun isActionAllowed(paidAction: IPaidAction): Boolean {
        return getActiveSubscriptions().any {
            it.subscription.actions.contains(paidAction)
        }
    }

    private fun setActiveSubscription(subscriptionId: String?) {
        prefs.edit().putString(KEY_ACTIVE_SUBSCRIPTION, subscriptionId).apply()

        activeSubscriptionStateFlow.update {
            getActiveSubscriptions().firstOrNull()
        }
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
                            billingPeriod = "P1M",
                            priceAmountMicros = 10000000L,
                            priceCurrencyCode = "USD"
                        )
                    ),
                    offerToken = "offerToken"
                ),
                BasePlan(
                    id = "plan-2",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$100.00",
                            billingPeriod = "P1Y",
                            priceAmountMicros = 100000000L,
                            priceCurrencyCode = "USD"
                        )
                    ),
                    offerToken = "offerToken"
                ),
            )
        } else {
            //VIP plans
            listOf(
                BasePlan(
                    id = "plan-0",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$0.00",
                            billingPeriod = "P1M",
                            priceAmountMicros = 0L,
                            priceCurrencyCode = "USD"
                        )
                    ),
                    offerToken = "offerToken"
                ),
                BasePlan(
                    id = "plan-1",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$19.9",
                            billingPeriod = "P1M",
                            priceAmountMicros = 19900000L,
                            priceCurrencyCode = "USD"
                        )
                    ),
                    offerToken = "offerToken"
                ),
                BasePlan(
                    id = "plan-2",
                    pricingPhases = listOf(
                        PricingPhase(
                            formattedPrice = "$149.9",
                            billingPeriod = "P1Y",
                            priceAmountMicros = 149900000L,
                            priceCurrencyCode = "USD"
                        )
                    ),
                    offerToken = "offerToken"
                ),
            )
        }

    override suspend fun getSubscriptions(): List<Subscription> = predefinedSubscriptions

    override fun getActiveSubscriptions(): List<UserSubscription> {
        val activeSubscription = prefs.getString(KEY_ACTIVE_SUBSCRIPTION, null)
        val subscriptions = predefinedSubscriptions.filter { it.id == activeSubscription }
        val testUniqueId = UUID.randomUUID().toString()
        return subscriptions.map { UserSubscription(it, testUniqueId) }
    }

    override suspend fun launchPurchaseFlow(
        subscriptionId: String,
        offerToken: String,
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
