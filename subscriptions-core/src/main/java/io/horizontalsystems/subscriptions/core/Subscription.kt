package io.horizontalsystems.subscriptions.core

data class Subscription(
    val id: String,
    val name: String,
    val description: String,
    val actions: List<IPaidAction>
)

data class BasePlan(
    val id: String,
    val pricingPhases: List<PricingPhase>,
)

data class PricingPhase(val formattedPrice: String, val billingPeriod: String)
