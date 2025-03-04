package cash.p.terminal.network.pirate.domain.enity

import java.math.BigDecimal

data class MarketTicker(
    val fromSymbol: String,
    val targetSymbol: String,
    val targetId: String,
    val market: String,
    val marketUrl: String,
    val tradeUrl: String,
    val price: BigDecimal,
    val priceUsd: BigDecimal,
    val volume: BigDecimal,
    val volumeUsd: BigDecimal,
    val volumePercent: BigDecimal,
    val trustScore: String
)
