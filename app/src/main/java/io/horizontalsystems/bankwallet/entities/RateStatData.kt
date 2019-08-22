package io.horizontalsystems.bankwallet.entities

import com.google.gson.annotations.SerializedName

data class RateStatData(
        @SerializedName("rates") val rates: List<Float>,
        @SerializedName("scale_minutes") val scale: Int,
        @SerializedName("timestamp") val timestamp: Long
)
