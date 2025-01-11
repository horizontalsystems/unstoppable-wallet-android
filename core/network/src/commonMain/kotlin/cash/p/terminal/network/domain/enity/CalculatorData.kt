package cash.p.terminal.network.domain.enity

data class CalculatorData(
    val items: List<CalculatorItemData>
)

data class CalculatorItemData(
    val periodType: PeriodType,
    val amount: Double,
    val price: Map<String, Double>,
)