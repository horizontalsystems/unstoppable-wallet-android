package cash.p.terminal.network.changenow.domain.entity

import java.math.BigDecimal

data class ExchangeAmount(
    val estimatedAmount: BigDecimal?,
    val transactionSpeedForecast: String?,
    val warningMessage: String?
)
