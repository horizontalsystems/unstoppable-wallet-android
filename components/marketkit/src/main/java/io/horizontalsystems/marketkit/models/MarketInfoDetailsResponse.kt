package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class MarketInfoDetailsResponse(
    val uid: String,
    val security: Security?,
    val tvl: BigDecimal?,
    @SerializedName("tvl_rank")
    val tvlRank: Int?,
    @SerializedName("tvl_ratio")
    val tvlRatio: BigDecimal?,
    @SerializedName("reports_count")
    val reportsCount: Int,
    @SerializedName("investor_data")
    val investorData: InvestorData?
) {
    data class Security(
        val privacy: String?,
        val decentralized: Boolean?,
        @SerializedName("censorship_resistance")
        val censorshipResistance: Boolean?,
        @SerializedName("confiscation_resistance")
        val confiscationResistance: Boolean?
    )

    data class InvestorData(
        @SerializedName("treasuries")
        val totalTreasuries: BigDecimal?,
        @SerializedName("funds_invested")
        val totalFundsInvested: BigDecimal?
    )
}
