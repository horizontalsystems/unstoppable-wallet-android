package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TokenHolders(
    val count: BigDecimal,
    @SerializedName("holders_url")
    val holdersUrl: String?,
    @SerializedName("top_holders")
    val topHolders: List<Holder>
)

data class Holder(
    val address: String,
    val percentage: BigDecimal,
    val balance: BigDecimal
)
