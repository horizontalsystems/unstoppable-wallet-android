package cash.p.terminal.network.changenow.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal class ExchangeAmountDto(
    val estimatedAmount: String?,
    val transactionSpeedForecast: String?,
    val warningMessage: String?
)
