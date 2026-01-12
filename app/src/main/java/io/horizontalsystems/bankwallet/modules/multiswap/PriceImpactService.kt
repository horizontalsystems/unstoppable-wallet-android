package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.ServiceState
import java.math.BigDecimal

class PriceImpactService : ServiceState<PriceImpactService.State>() {
    private var amountIn: BigDecimal? = null
    private var amountOut: BigDecimal? = null
    private var priceImpact: BigDecimal? = null
    private var priceImpactLevel: PriceImpactLevel? = null

    private var providerTitle: String? = null

    override fun createState() = State(
        priceImpact = priceImpact,
        priceImpactLevel = priceImpactLevel
    )

    fun setProviderTitle(providerTitle: String?) {
        this.providerTitle = providerTitle

        emitState()
    }

    private fun refreshPriceImpact() {
        val priceImpactData = PriceImpactCalculator.getPriceImpactData(amountOut, amountIn)

        priceImpact = priceImpactData?.priceImpact
        priceImpactLevel = priceImpactData?.priceImpactLevel
    }

    fun setAmountIn(amountIn: BigDecimal?) {
        this.amountIn = amountIn

        refreshPriceImpact()

        emitState()
    }

    fun setAmountOut(amountOut: BigDecimal?) {
        this.amountOut = amountOut

        refreshPriceImpact()

        emitState()
    }

    data class State(
        val priceImpact: BigDecimal?,
        val priceImpactLevel: PriceImpactLevel?
    )
}
