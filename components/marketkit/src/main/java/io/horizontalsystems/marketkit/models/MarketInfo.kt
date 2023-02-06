package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class MarketInfo(
    val fullCoin: FullCoin,
    val price: BigDecimal?,
    val priceChange24h: BigDecimal?,
    val priceChange7d: BigDecimal?,
    val priceChange14d: BigDecimal?,
    val priceChange30d: BigDecimal?,
    val priceChange200d: BigDecimal?,
    val priceChange1y: BigDecimal?,
    val marketCap: BigDecimal?,
    val marketCapRank: Int?,
    val totalVolume: BigDecimal?,
    val athPercentage: BigDecimal?,
    val atlPercentage: BigDecimal?
) {
    constructor(marketInfoRaw: MarketInfoRaw, fullCoin: FullCoin) : this(
        fullCoin,
        marketInfoRaw.price,
        marketInfoRaw.priceChange24h,
        marketInfoRaw.priceChange7d,
        marketInfoRaw.priceChange14d,
        marketInfoRaw.priceChange30d,
        marketInfoRaw.priceChange200d,
        marketInfoRaw.priceChange1y,
        marketInfoRaw.marketCap,
        marketInfoRaw.marketCapRank,
        marketInfoRaw.totalVolume,
        marketInfoRaw.athPercentage,
        marketInfoRaw.atlPercentage,
    )
}
