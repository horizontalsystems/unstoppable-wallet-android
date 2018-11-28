package io.horizontalsystems.bankwallet.entities

import com.google.gson.annotations.SerializedName

data class LatestRate(@SerializedName("rate") val value: Double, @SerializedName("date") val timestamp: Long)
