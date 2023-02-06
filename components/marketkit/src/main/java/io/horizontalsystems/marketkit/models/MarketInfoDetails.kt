package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class MarketInfoDetails(
    val tvl: BigDecimal?,
    val tvlRank: Int?,
    val tvlRatio: BigDecimal?,
    val totalTreasuries: BigDecimal?,
    val totalFundsInvested: BigDecimal?,
    val reportsCount: Int,
    val privacy: SecurityLevel?,
    val decentralizedIssuance: Boolean?,
    val confiscationResistant: Boolean?,
    val censorshipResistant: Boolean?
) {
    constructor(response: MarketInfoDetailsResponse) : this(
        response.tvl,
        response.tvlRank,
        response.tvlRatio,
        response.investorData?.totalTreasuries,
        response.investorData?.totalFundsInvested,
        response.reportsCount,
        response.security?.privacy?.let { SecurityLevel.fromString(it) },
        response.security?.decentralized,
        response.security?.confiscationResistance,
        response.security?.censorshipResistance
    )

    enum class SecurityLevel(val v: String) {
        Low("low"), Medium("medium"), High("high");

        companion object {
            private val map = SecurityLevel.values().associateBy(SecurityLevel::v)

            fun fromString(v: String) = map[v]
        }
    }
}
