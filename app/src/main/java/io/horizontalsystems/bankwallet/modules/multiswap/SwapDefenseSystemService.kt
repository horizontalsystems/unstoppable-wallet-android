package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemState
import java.math.BigDecimal

class SwapDefenseSystemService : ServiceState<SwapDefenseSystemService.State>() {
    private var fiatPriceImpact: BigDecimal? = null
    private var fiatPriceImpactLevel: PriceImpactLevel? = null

    private var systemMessage: DefenseSystemMessage? = null

    override fun createState() = State(
        systemMessage = systemMessage,
    )

    fun setPriceImpact(fiatPriceImpact: BigDecimal?, fiatPriceImpactLevel: PriceImpactLevel?) {
        this.fiatPriceImpact = fiatPriceImpact
        this.fiatPriceImpactLevel = fiatPriceImpactLevel

        refresh()

        emitState()
    }

    private fun refresh() {
        val fiatPriceImpact = fiatPriceImpact
        val fiatPriceImpactLevel = fiatPriceImpactLevel

        systemMessage = if (fiatPriceImpact != null && fiatPriceImpactLevel != null) {
            when (fiatPriceImpactLevel) {
                PriceImpactLevel.High -> {
                    DefenseSystemMessage(
                        level = DefenseSystemState.DANGER,
                        title = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_High_Title),
                        body = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_High_Description, fiatPriceImpact),
                    )
                }
                PriceImpactLevel.Forbidden -> {
                    DefenseSystemMessage(
                        level = DefenseSystemState.DANGER,
                        title = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_Forbidden_Title),
                        body = TranslatableString.ResString(R.string.SwapDefense_PriceImpact_Forbidden_Description, fiatPriceImpact),
                    )
                }
                else -> {
                    null
                }
            }
        } else {
            null
        }
    }

    data class State(
        val systemMessage: DefenseSystemMessage?
    )
}
