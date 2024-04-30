package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TopPair(
    val rank: Long,
    val base: String,
    val target: String,
    val price: BigDecimal?,
    val volume: BigDecimal,
    @SerializedName("market_name")
    val marketName: String?,
    @SerializedName("market_logo")
    val marketLogo: String?,
    @SerializedName("trade_url")
    val tradeUrl: String?,
)
