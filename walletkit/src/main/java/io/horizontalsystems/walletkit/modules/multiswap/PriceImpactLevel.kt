package io.horizontalsystems.walletkit.modules.multiswap


enum class PriceImpactLevel(val lowerInclusive: Int, val upperExclusive: Int) {
    Normal(1, 5),
    Warning(5, 10),
    High(10, 50),
    Forbidden(50, Int.MAX_VALUE);

    companion object {
        fun valuesSorted() = listOf(Normal, Warning, High, Forbidden)
    }
}
