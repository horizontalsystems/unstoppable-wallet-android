package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class CoinTreasuryResponse(
    val type: String,
    val fund: String,
    @SerializedName("fund_uid")
    val fundUid: String,
    val amount: BigDecimal,
    @SerializedName("amount_in_currency")
    val amountInCurrency: BigDecimal,
    @SerializedName("country")
    val countryCode: String
)
