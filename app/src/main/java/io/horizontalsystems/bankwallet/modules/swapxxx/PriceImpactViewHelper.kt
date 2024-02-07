package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
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
