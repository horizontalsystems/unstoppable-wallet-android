package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class TopPlatform(
    val blockchain: Blockchain,
    val rank: Int,
    val protocols: Int,
    val marketCap: BigDecimal,
    val rank1W: Int?,
    val rank1M: Int?,
    val rank3M: Int?,
    val change1W: BigDecimal?,
    val change1M: BigDecimal?,
    val change3M: BigDecimal?
)
