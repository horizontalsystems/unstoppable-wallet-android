package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class CoinCategoryMarketPoint(
    val timestamp: Long,
    @SerializedName("market_cap")
    val marketCap: BigDecimal,
)
