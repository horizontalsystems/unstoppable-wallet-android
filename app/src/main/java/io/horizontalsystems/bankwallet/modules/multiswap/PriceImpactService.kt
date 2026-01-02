package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import java.math.BigDecimal
import java.math.RoundingMode

class PriceImpactService : ServiceState<PriceImpactService.State>() {
    private val normalPriceImpact = BigDecimal(1)
    private val warningPriceImpact = BigDecimal(6)
    private val highPriceImpact = BigDecimal(11)
    private val forbiddenPriceImpact = BigDecimal(50)

    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null
    private var fiatPriceImpactCaution: HSCaution? = null

    private var providerTitle: String? = null

    override fun createState() = State(
        fiatPriceImpact = fiatPriceImpact,
        fiatPriceImpactLevel = fiatPriceImpactLevel,
        fiatPriceImpactCaution = fiatPriceImpactCaution
    )

    fun setProviderTitle(providerTitle: String?) {
        this.providerTitle = providerTitle

        emitState()
    }

    private fun refreshFiatPriceImpact() {
        val fiatAmountIn = fiatAmountIn
        val fiatAmountOut = fiatAmountOut

        val fiatPriceImpact = calculateDiff(fiatAmountOut, fiatAmountIn)
        val fiatPriceImpactAbs = fiatPriceImpact?.abs()

        if (fiatPriceImpactAbs == null || fiatPriceImpactAbs < normalPriceImpact) {
            this.fiatPriceImpact = null
            fiatPriceImpactLevel = null
        } else {
            this.fiatPriceImpact = fiatPriceImpact
            fiatPriceImpactLevel = when {
                fiatPriceImpactAbs < warningPriceImpact -> PriceImpactLevel.Normal
                fiatPriceImpactAbs < highPriceImpact -> PriceImpactLevel.Warning
                fiatPriceImpactAbs < forbiddenPriceImpact -> PriceImpactLevel.High
                else -> PriceImpactLevel.Forbidden
            }

            fiatPriceImpactCaution = when (fiatPriceImpactLevel) {
                PriceImpactLevel.Forbidden -> {
                    HSCaution(
                        s = TranslatableString.ResString(R.string.Swap_PriceImpact),
                        type = HSCaution.Type.Error,
                        description = TranslatableString.ResString(R.string.Swap_PriceImpactTooHigh, providerTitle ?: "")
                    )
                }
                PriceImpactLevel.Warning -> {
                    HSCaution(
                        s = TranslatableString.ResString(R.string.Swap_PriceImpact),
                        type = HSCaution.Type.Warning,
                        description = TranslatableString.ResString(R.string.Swap_PriceImpactWarning)
                    )
                }
                else -> {
                    null
                }
            }
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

    fun setFiatAmountIn(fiatAmountIn: BigDecimal?) {
        this.fiatAmountIn = fiatAmountIn

        refreshFiatPriceImpact()

        emitState()
    }

    fun setFiatAmountOut(fiatAmountOut: BigDecimal?) {
        this.fiatAmountOut = fiatAmountOut

        refreshFiatPriceImpact()

        emitState()
    }

    data class State(
        val fiatPriceImpact: BigDecimal?,
        val fiatPriceImpactLevel: PriceImpactLevel?,
        val fiatPriceImpactCaution: HSCaution?
    )
}
