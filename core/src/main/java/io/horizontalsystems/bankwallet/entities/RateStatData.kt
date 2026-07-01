package io.horizontalsystems.bankwallet.entities

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class RateData(
        @SerializedName("rates") val rates: List<Float>,
        @SerializedName("scale_minutes") val scale: Int,
        @SerializedName("timestamp") val timestamp: Long
)

data class RateStatData(
        @SerializedName("market_cap") val marketCap: BigDecimal,
        @SerializedName("stats") val stats: Map<String, RateData>
)
