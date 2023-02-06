package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class GlobalMarketPoint(
    @SerializedName("date")
    val timestamp: Long,
    @SerializedName("market_cap")
    val marketCap: BigDecimal,
    @SerializedName("defi_market_cap")
    val defiMarketCap: BigDecimal,
    @SerializedName("volume")
    val volume24h: BigDecimal,
    @SerializedName("btc_dominance")
    val btcDominance: BigDecimal,
    val tvl: BigDecimal,
)
