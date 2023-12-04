package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R

enum class BitcoinCashCoinType(val value: String) {
    type0("type0"), type145("type145");

    val title: String
        get() {
            return when (this) {
                type0 -> "Legacy (Type 0)"
                type145 -> "CashAddress (Type 145)"
            }
        }

    val description: Int
        get() {
            return when (this) {
                type0 -> R.string.CoinSettings_BitcoinCashCoinType_Type0_Description
                type145 -> R.string.CoinSettings_BitcoinCashCoinType_Type145_Description
            }
        }

        companion object {
            val default = type145
            private val map = values().associateBy(BitcoinCashCoinType::value)

            fun fromString(value: String?): BitcoinCashCoinType? = map[value]
        }
}
