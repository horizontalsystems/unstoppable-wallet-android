package io.horizontalsystems.walletkit.entities

import com.google.gson.annotations.SerializedName

data class LatestRateData(
        val rates: Map<String, String>,
        val currency: String,
        @SerializedName("time") val timestamp: Long
)
