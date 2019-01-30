package io.horizontalsystems.bankwallet.entities

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class LatestRate(@SerializedName("rate") val value: BigDecimal, @SerializedName("date") val timestamp: Long)
