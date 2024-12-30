package cash.p.terminal.featureStacking.ui.calculatorScreen

import cash.p.terminal.featureStacking.ui.staking.StackingType

data class CalculatorUIState(
    val amount: String = "100",
    val coin: String = StackingType.PCASH.value.lowercase(),
    val coinSecondary: String = "",
    val coinExchange: String = "",
    val calculateResult: List<CalculatorItem> = emptyList()
)
