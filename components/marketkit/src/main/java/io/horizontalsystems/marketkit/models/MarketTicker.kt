package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class MarketTicker(
    val base: String,
    val target: String,
    val marketName: String,
    val marketImageUrl: String?,
    val rate: BigDecimal,
    val volume: BigDecimal,
    val tradeUrl: String?,
)
