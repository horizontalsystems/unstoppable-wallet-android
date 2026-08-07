package io.horizontalsystems.walletkit.core.providers

data class FeeRates(
    val recommended: Int,
    val minimum: Int = 0,
)
