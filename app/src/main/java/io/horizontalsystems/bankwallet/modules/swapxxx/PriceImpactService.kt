package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

class PriceImpactService {
    private val normalPriceImpact = BigDecimal(1)
    private val warningPriceImpact = BigDecimal(5)
    private val forbiddenPriceImpact = BigDecimal(20)

    private var priceImpact: BigDecimal? = null
    private var priceImpactLevel: SwapMainModule.PriceImpactLevel? = null
    private var priceImpactCaution: HSCaution? = null

    private val _stateFlow = MutableStateFlow(
        State(
            priceImpact = priceImpact,
            priceImpactLevel = priceImpactLevel,
            priceImpactCaution = priceImpactCaution,
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    fun setPriceImpact(priceImpact: BigDecimal?, providerTitle: String?) {
        this.priceImpact = priceImpact

        priceImpactLevel = when {
            priceImpact == null -> null
            priceImpact >= BigDecimal.ZERO && priceImpact < normalPriceImpact -> SwapMainModule.PriceImpactLevel.Negligible
            priceImpact >= normalPriceImpact && priceImpact < warningPriceImpact -> SwapMainModule.PriceImpactLevel.Normal
            priceImpact >= warningPriceImpact && priceImpact < forbiddenPriceImpact -> SwapMainModule.PriceImpactLevel.Warning
            else -> SwapMainModule.PriceImpactLevel.Forbidden
        }

        priceImpactCaution = when (priceImpactLevel) {
            SwapMainModule.PriceImpactLevel.Forbidden -> {
                HSCaution(
                    s = TranslatableString.ResString(R.string.Swap_PriceImpact),
                    type = HSCaution.Type.Error,
                    description = TranslatableString.ResString(R.string.Swap_PriceImpactTooHigh, providerTitle ?: "")
                )
            }
            SwapMainModule.PriceImpactLevel.Warning -> {
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

        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(priceImpact, priceImpactLevel, priceImpactCaution)
        }
    }

    data class State(
        val priceImpact: BigDecimal?,
        val priceImpactLevel: SwapMainModule.PriceImpactLevel?,
        val priceImpactCaution: HSCaution?
    )
}
