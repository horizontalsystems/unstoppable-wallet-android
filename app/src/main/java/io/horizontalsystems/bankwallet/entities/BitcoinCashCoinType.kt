package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R

enum class BitcoinCashCoinType(val value: String) {
    type0("type0"), type145("type145");

    val title: Int
        get() {
            return when (this) {
                type0 -> R.string.CoinSettings_BitcoinCashCoinType_Type0_Title
                type145 -> R.string.CoinSettings_BitcoinCashCoinType_Type145_Title
            }
        }

    val description: Int
        get() {
            return when (this) {
                type0 -> R.string.CoinSettings_BitcoinCashCoinType_Type0_Description
                type145 -> R.string.CoinSettings_BitcoinCashCoinType_Type145_Description
            }
        }
}
