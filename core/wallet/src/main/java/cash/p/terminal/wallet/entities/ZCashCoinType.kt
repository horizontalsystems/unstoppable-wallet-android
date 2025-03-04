package cash.p.terminal.wallet.entities

import cash.p.terminal.wallet.R

enum class ZCashCoinType(val value: String) {
    Shielded("shielded"),
    Transparent("transparent"),
    Unified("unified");

    val title: String
        get() {
            return when (this) {
                Shielded -> "Shielded"
                Transparent -> "Transparent"
                Unified -> "Unified"
            }
        }

    val description: Int
        get() {
            return when (this) {
                Shielded -> R.string.zcash_shielded_description
                Transparent -> R.string.zcash_transparent_description
                Unified -> R.string.zcash_unified_description
            }
        }

    companion object {
        private val map = values().associateBy(ZCashCoinType::value)

        fun fromString(value: String?): ZCashCoinType? = map[value]
    }
}
