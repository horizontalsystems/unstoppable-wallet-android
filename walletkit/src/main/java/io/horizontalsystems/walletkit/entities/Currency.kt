package io.horizontalsystems.walletkit.entities


data class Currency(
    val code: String,
    val symbol: String,
    val decimal: Int,
    val flag: Int
)
