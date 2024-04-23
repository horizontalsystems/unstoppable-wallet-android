package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.RoundingMode

class PriceImpactService {
    private val normalPriceImpact = BigDecimal(1)
    private val warningPriceImpact = BigDecimal(5)
    private val forbiddenPriceImpact = BigDecimal(20)

    private var fiatAmountIn: BigDecimal? = null
    private var fiatAmountOut: BigDecimal? = null
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null

    private var priceImpact: BigDecimal? = null
    private var priceImpactLevel: PriceImpactLevel? = null
    private var priceImpactCaution: HSCaution? = null
    private var error: Throwable? = null

    private val _stateFlow = MutableStateFlow(
        State(
            priceImpact = priceImpact,
            priceImpactLevel = priceImpactLevel,
            priceImpactCaution = priceImpactCaution,
            fiatPriceImpact = fiatPriceImpact,
            fiatPriceImpactLevel = fiatPriceImpactLevel,
            error = error
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setPriceImpact(priceImpact: BigDecimal?, providerTitle: String?) {
        if (priceImpact == null || priceImpact < normalPriceImpact) {
            this.priceImpact = null
            priceImpactLevel = null
            priceImpactCaution = null
        } else {
            this.priceImpact = priceImpact

            priceImpactLevel = when {
                priceImpact < warningPriceImpact -> PriceImpactLevel.Normal
                priceImpact < forbiddenPriceImpact -> PriceImpactLevel.Warning
                else -> PriceImpactLevel.Forbidden
            }

            priceImpactCaution = when (priceImpactLevel) {
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

        error = if (priceImpactLevel == PriceImpactLevel.Forbidden) {
            PriceImpactTooHigh(providerTitle)
        } else {
            null
        }

        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                priceImpact,
                priceImpactLevel,
                priceImpactCaution,
                fiatPriceImpact,
                fiatPriceImpactLevel,
                error
            )
        }
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
                fiatPriceImpactAbs < forbiddenPriceImpact -> PriceImpactLevel.Warning
                else -> PriceImpactLevel.Forbidden
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
        val priceImpact: BigDecimal?,
        val priceImpactLevel: PriceImpactLevel?,
        val priceImpactCaution: HSCaution?,
        val fiatPriceImpact: BigDecimal?,
        val fiatPriceImpactLevel: PriceImpactLevel?,
        val error: Throwable?
    )
}

data class PriceImpactTooHigh(val providerTitle: String?) : Exception()