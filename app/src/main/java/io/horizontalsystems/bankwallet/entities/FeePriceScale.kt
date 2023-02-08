package io.horizontalsystems.bankwallet.entities

enum class FeePriceScale {
    Satoshi, Gwei, Navax;

    val unit: String
        get() = when (this) {
            Satoshi -> "satoshi"
            Gwei -> "Gwei"
            Navax -> "nAvax"
        }

    val scaleValue: Int
        get() = when (this) {
            Satoshi -> 1
            Gwei, Navax -> 1_000_000_000
        }

    val decimals: Int
        get() = when(this) {
            Satoshi -> 1
            Gwei, Navax -> 9
        }
}