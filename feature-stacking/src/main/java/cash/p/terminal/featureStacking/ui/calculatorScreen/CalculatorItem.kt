package cash.p.terminal.featureStacking.ui.calculatorScreen

import cash.p.terminal.network.pirate.domain.enity.PeriodType

data class CalculatorItem(
    val period: PeriodType,
    val amount: String,
    val amountSecondary: String
)
