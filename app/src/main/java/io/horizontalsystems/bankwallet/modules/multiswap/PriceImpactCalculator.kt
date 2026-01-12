package io.horizontalsystems.bankwallet.modules.multiswap

import java.math.BigDecimal
import java.math.RoundingMode

object PriceImpactCalculator {
    private val normalPriceImpact = BigDecimal(1)
    private val warningPriceImpact = BigDecimal(6)
    private val highPriceImpact = BigDecimal(11)
    private val forbiddenPriceImpact = BigDecimal(50)

    fun getPriceImpactData(amountOut: BigDecimal?, amountIn: BigDecimal?): PriceImpactData? {
        val priceImpact = calculateDiff(amountOut, amountIn) ?: return null

        val priceImpactAbs = priceImpact.abs()
        if (priceImpactAbs < normalPriceImpact) return null

        val priceImpactLevel = when {
            priceImpactAbs < warningPriceImpact -> PriceImpactLevel.Normal
            priceImpactAbs < highPriceImpact -> PriceImpactLevel.Warning
            priceImpactAbs < forbiddenPriceImpact -> PriceImpactLevel.High
            else -> PriceImpactLevel.Forbidden
        }

        return PriceImpactData(priceImpact, priceImpactLevel)
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