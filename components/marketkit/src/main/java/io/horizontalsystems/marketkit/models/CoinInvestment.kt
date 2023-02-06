package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*

data class CoinInvestment(
    val date: Date,
    val round: String,
    val amount: BigDecimal?,
    val funds: List<Fund>
) {

    data class Fund(
        val uid: String,
        val name: String,
        val website: String,
        @SerializedName("is_lead")
        val isLead: Boolean
    )
}
