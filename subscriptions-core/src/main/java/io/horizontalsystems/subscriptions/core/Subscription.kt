package io.horizontalsystems.subscriptions.core

import java.time.Period

data class Subscription(
    val id: String,
    val name: String,
    val description: String,
    val actions: List<IPaidAction>
)

data class BasePlan(
    val id: String,
    val pricingPhases: List<PricingPhase>,
    val offerToken: String,
) {
    val feePricingPhase = pricingPhases.find { it.isFree }
    val hasFreeTrial = feePricingPhase != null
}

data class PricingPhase(
    val formattedPrice: String,
    val billingPeriod: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
) {
    val isFree = priceAmountMicros == 0L
    val period: Period = Period.parse(billingPeriod)
    val numberOfDays = period.numberOfDays()
}

data class UserSubscription(
    val subscription: Subscription,
    val purchaseToken: String
)

fun Period.numberOfDays() = years * 365 + months * 30 + days