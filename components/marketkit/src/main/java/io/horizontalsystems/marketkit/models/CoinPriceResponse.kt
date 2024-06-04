package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class CoinPriceResponse(
    val uid: String,
    val price: BigDecimal?,
    @SerializedName("price_change_24h")
    val priceChange24h: BigDecimal?,
    @SerializedName("price_change_1d")
    val priceChange1d: BigDecimal?,
    @SerializedName("last_updated")
    val lastUpdated: Long?
) {
    fun coinPrice(currencyCode: String) = when {
        price == null || lastUpdated == null -> null
        else -> CoinPrice(uid, currencyCode, price, priceChange24h, priceChange1d, lastUpdated)
    }
}
