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
    val volume: BigDecimal,
    @SerializedName("volume_in_currency")
    val fiatVolume: BigDecimal,
    @SerializedName("trade_url")
    val tradeUrl: String?,
    @SerializedName("whitelisted")
    val verified: Boolean
)
