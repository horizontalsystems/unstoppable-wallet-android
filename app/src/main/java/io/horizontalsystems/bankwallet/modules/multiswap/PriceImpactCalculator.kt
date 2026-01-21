package io.horizontalsystems.bankwallet.modules.multiswap

import java.math.BigDecimal
import java.math.RoundingMode

object PriceImpactCalculator {
    fun getPriceImpactData(
        amountOut: BigDecimal?,
        amountIn: BigDecimal?,
        minLevel: PriceImpactLevel
    ): PriceImpactData? {
        val priceImpact = calculateDiff(amountOut, amountIn) ?: return null

        val priceImpactAbs = priceImpact.abs()
        if (priceImpactAbs < minLevel.lowerInclusive.toBigDecimal()) return null

        return PriceImpactLevel.valuesSorted().firstOrNull {
            priceImpactAbs >= it.lowerInclusive.toBigDecimal() && priceImpactAbs < it.upperExclusive.toBigDecimal()
        }?.let {
            PriceImpactData(priceImpact, it)
        }
    }

    private fun calculateDiff(amountOut: BigDecimal?, amountIn: BigDecimal?): BigDecimal? {
        if (amountOut == null || amountIn == null || amountIn.compareTo(BigDecimal.ZERO) == 0) return null

        return (amountOut - amountIn)
            .divide(amountIn, RoundingMode.DOWN)
            .times(BigDecimal("100"))
            .setScale(2, RoundingMode.DOWN)
            .stripTrailingZeros()
    }
}

data class PriceImpactData(
    val priceImpact: BigDecimal,
    val priceImpactLevel: PriceImpactLevel? = null
)