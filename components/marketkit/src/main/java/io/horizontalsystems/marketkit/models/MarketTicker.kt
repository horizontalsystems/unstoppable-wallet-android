package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class MarketTicker(
    val base: String,
    val target: String,
    @SerializedName("market_name")
    val marketName: String,
    @SerializedName("market_logo")
    val marketImageUrl: String?,
    @SerializedName("price")
    val rate: BigDecimal,
    val volume: BigDecimal,
    @SerializedName("trade_url")
    val tradeUrl: String?,
    @SerializedName("whitelisted")
    val verified: Boolean
)
