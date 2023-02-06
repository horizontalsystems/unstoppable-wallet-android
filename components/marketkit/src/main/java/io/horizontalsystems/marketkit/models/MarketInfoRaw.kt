package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class MarketInfoRaw(
    val uid: String,
    val price: BigDecimal?,
    @SerializedName("price_change_24h")
    val priceChange24h: BigDecimal?,
    @SerializedName("price_change_7d")
    val priceChange7d: BigDecimal?,
    @SerializedName("price_change_14d")
    val priceChange14d: BigDecimal?,
    @SerializedName("price_change_30d")
    val priceChange30d: BigDecimal?,
    @SerializedName("price_change_200d")
    val priceChange200d: BigDecimal?,
    @SerializedName("price_change_1y")
    val priceChange1y: BigDecimal?,
    @SerializedName("market_cap")
    val marketCap: BigDecimal?,
    @SerializedName("market_cap_rank")
    val marketCapRank: Int?,
    @SerializedName("total_volume")
    val totalVolume: BigDecimal?,
    @SerializedName("ath_percentage")
    val athPercentage: BigDecimal?,
    @SerializedName("atl_percentage")
    val atlPercentage: BigDecimal?,
)
