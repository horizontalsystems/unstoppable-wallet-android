package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import java.math.BigDecimal

class PriceImpactService : ServiceState<PriceImpactService.State>() {
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
        val priceImpactData = PriceImpactCalculator.getPriceImpactData(fiatAmountOut, fiatAmountIn)

        fiatPriceImpact = priceImpactData?.priceImpact
        fiatPriceImpactLevel = priceImpactData?.priceImpactLevel

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
