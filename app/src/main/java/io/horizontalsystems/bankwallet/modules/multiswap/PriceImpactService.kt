package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.ServiceState
import java.math.BigDecimal

class PriceImpactService : ServiceState<PriceImpactService.State>() {
    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null

    private var providerTitle: String? = null

    override fun createState() = State(
        fiatPriceImpact = fiatPriceImpact,
        fiatPriceImpactLevel = fiatPriceImpactLevel
    )

    fun setProviderTitle(providerTitle: String?) {
        this.providerTitle = providerTitle

        emitState()
    }

    private fun refreshFiatPriceImpact() {
        val priceImpactData = PriceImpactCalculator.getPriceImpactData(fiatAmountOut, fiatAmountIn)

        fiatPriceImpact = priceImpactData?.priceImpact
        fiatPriceImpactLevel = priceImpactData?.priceImpactLevel
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
        val fiatPriceImpactLevel: PriceImpactLevel?
    )
}
