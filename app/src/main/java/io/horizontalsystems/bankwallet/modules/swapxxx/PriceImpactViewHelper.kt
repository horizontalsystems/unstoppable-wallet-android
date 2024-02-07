package cash.p.terminal.modules.swapxxx

import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.modules.swap.SwapMainModule
import java.math.BigDecimal

object PriceImpactViewHelper {
    private val normalPriceImpact = BigDecimal(1)
    private val warningPriceImpact = BigDecimal(5)
    private val forbiddenPriceImpact = BigDecimal(20)

    fun getPriceImpactViewItem(priceImpact: BigDecimal): SwapMainModule.PriceImpactViewItem {
        val priceImpactLevel = when {
            priceImpact >= BigDecimal.ZERO && priceImpact < normalPriceImpact -> SwapMainModule.PriceImpactLevel.Negligible
            priceImpact >= normalPriceImpact && priceImpact < warningPriceImpact -> SwapMainModule.PriceImpactLevel.Normal
            priceImpact >= warningPriceImpact && priceImpact < forbiddenPriceImpact -> SwapMainModule.PriceImpactLevel.Warning
            else -> SwapMainModule.PriceImpactLevel.Forbidden
        }

        return SwapMainModule.PriceImpactViewItem(priceImpactLevel, Translator.getString(R.string.Swap_Percent, priceImpact * BigDecimal.valueOf(-1)))
    }
}
