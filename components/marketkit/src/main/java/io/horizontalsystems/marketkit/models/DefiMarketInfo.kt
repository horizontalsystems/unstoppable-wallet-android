package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class DefiMarketInfo(
    val fullCoin: FullCoin?,
    val name: String,
    val logoUrl: String,
    val tvl: BigDecimal,
    val tvlRank: Int,
    val tvlChange1D: BigDecimal?,
    val tvlChange1W: BigDecimal?,
    val tvlChange2W: BigDecimal?,
    val tvlChange1M: BigDecimal?,
    val tvlChange3M: BigDecimal?,
    val tvlChange6M: BigDecimal?,
    val tvlChange1Y: BigDecimal?,
    val chains: List<String>,
    val chainTvls: Map<String, BigDecimal?>,
) {
    constructor(defiMarketInfoResponse: DefiMarketInfoResponse, fullCoin: FullCoin?) : this(
        fullCoin,
        defiMarketInfoResponse.name,
        defiMarketInfoResponse.logoUrl,
        defiMarketInfoResponse.tvl,
        defiMarketInfoResponse.tvlRank,
        defiMarketInfoResponse.tvlChange1D,
        defiMarketInfoResponse.tvlChange1W,
        defiMarketInfoResponse.tvlChange2W,
        defiMarketInfoResponse.tvlChange1M,
        defiMarketInfoResponse.tvlChange3M,
        defiMarketInfoResponse.tvlChange6M,
        defiMarketInfoResponse.tvlChange1Y,
        defiMarketInfoResponse.chains,
        defiMarketInfoResponse.chainTvls,
    )
}
