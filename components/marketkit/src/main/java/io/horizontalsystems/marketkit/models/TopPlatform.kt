package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class TopPlatform(
    val blockchain: Blockchain,
    val rank: Int,
    val protocols: Int,
    val marketCap: BigDecimal,
    val rank1D: Int?,
    val rank1W: Int?,
    val rank1M: Int?,
    val change1D: BigDecimal?,
    val change1W: BigDecimal?,
    val change1M: BigDecimal?
)
