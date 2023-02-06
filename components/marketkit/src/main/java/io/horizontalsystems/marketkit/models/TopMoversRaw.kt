package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName

data class TopMoversRaw(
    @SerializedName("gainers_100")
    val gainers100: List<MarketInfoRaw>,
    @SerializedName("gainers_200")
    val gainers200: List<MarketInfoRaw>,
    @SerializedName("gainers_300")
    val gainers300: List<MarketInfoRaw>,
    @SerializedName("losers_100")
    val losers100: List<MarketInfoRaw>,
    @SerializedName("losers_200")
    val losers200: List<MarketInfoRaw>,
    @SerializedName("losers_300")
    val losers300: List<MarketInfoRaw>
)
