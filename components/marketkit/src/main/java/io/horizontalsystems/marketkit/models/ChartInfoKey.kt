package io.horizontalsystems.marketkit.models

data class ChartInfoKey(
    val coinUid: String,
    val currencyCode: String,
    val periodType: HsPeriodType
)
