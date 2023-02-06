package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class CoinTreasury(
    val type: TreasuryType,
    val fund: String,
    val fundUid: String,
    val amount: BigDecimal,
    val amountInCurrency: BigDecimal,
    val countryCode: String
) {
    enum class TreasuryType(val v: String) {
        Private("private"), Public("public"), Etf("etf");

        companion object {
            private val map = values().associateBy(TreasuryType::v)

            fun fromString(v: String) = map[v]
        }
    }
}
