package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.Serializable

@Serializable
data class CalculatorDataDto(
    val day: CalculatorItemDataDto,
    val week: CalculatorItemDataDto,
    val month: CalculatorItemDataDto,
    val year: CalculatorItemDataDto
)

@Serializable
data class CalculatorItemDataDto(
    val amount: Double,
    val price: Map<String, Double>,
)