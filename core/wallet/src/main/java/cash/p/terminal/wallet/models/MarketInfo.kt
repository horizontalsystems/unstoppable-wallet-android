package cash.p.terminal.wallet.models

import java.math.BigDecimal

data class MarketInfo(
    val fullCoin: cash.p.terminal.wallet.entities.FullCoin,
    val price: BigDecimal?,
    val priceChange1d: BigDecimal?,
    val priceChange24h: BigDecimal?,
    val priceChange7d: BigDecimal?,
    val priceChange14d: BigDecimal?,
    val priceChange30d: BigDecimal?,
    val priceChange90d: BigDecimal?,
    val priceChange200d: BigDecimal?,
    val priceChange1y: BigDecimal?,
    val marketCap: BigDecimal?,
    val marketCapRank: Int?,
    val totalVolume: BigDecimal?,
    val athPercentage: BigDecimal?,
    val atlPercentage: BigDecimal?,
    val listedOnTopExchanges: Boolean?,
    val solidCex: Boolean?,
    val solidDex: Boolean?,
    val goodDistribution: Boolean?,
    val advice: Analytics.TechnicalAdvice.Advice?,
) {
    constructor(marketInfoRaw: MarketInfoRaw, fullCoin: cash.p.terminal.wallet.entities.FullCoin) : this(
        fullCoin,
        marketInfoRaw.price,
        marketInfoRaw.priceChange1d,
        marketInfoRaw.priceChange24h,
        marketInfoRaw.priceChange7d,
        marketInfoRaw.priceChange14d,
        marketInfoRaw.priceChange30d,
        marketInfoRaw.priceChange90d,
        marketInfoRaw.priceChange200d,
        marketInfoRaw.priceChange1y,
        marketInfoRaw.marketCap,
        marketInfoRaw.marketCapRank,
        marketInfoRaw.totalVolume,
        marketInfoRaw.athPercentage,
        marketInfoRaw.atlPercentage,
        marketInfoRaw.listedOnTopExchanges,
        marketInfoRaw.solidCex,
        marketInfoRaw.solidDex,
        marketInfoRaw.goodDistribution,
        marketInfoRaw.advice,
    )
}
