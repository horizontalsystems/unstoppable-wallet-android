package cash.p.terminal.featureStacking.ui.calculatorScreen

import cash.p.terminal.featureStacking.ui.staking.StackingType

internal data class CalculatorUIState(
    val amount: String = "100",
    val stackingType: StackingType = StackingType.PCASH,
    val coinSecondary: String = "",
    val coinExchange: String = "",
    val calculateResult: List<CalculatorItem> = emptyList()
)
