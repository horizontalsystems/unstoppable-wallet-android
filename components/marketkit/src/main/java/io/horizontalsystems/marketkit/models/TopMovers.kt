package io.horizontalsystems.marketkit.models

data class TopMovers(
    val gainers100: List<MarketInfo>,
    val gainers200: List<MarketInfo>,
    val gainers300: List<MarketInfo>,
    val losers100: List<MarketInfo>,
    val losers200: List<MarketInfo>,
    val losers300: List<MarketInfo>
)
