package io.horizontalsystems.marketkit.models

data class ChartInfoKey(
        val coin: Coin,
        val currencyCode: String,
        val periodType: HsPeriodType
)
