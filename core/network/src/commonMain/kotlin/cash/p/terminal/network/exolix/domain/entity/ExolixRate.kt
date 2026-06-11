package cash.p.terminal.network.exolix.domain.entity

import java.math.BigDecimal

data class ExolixRate(
    val fromAmount: BigDecimal,
    val toAmount: BigDecimal,
    val rate: BigDecimal,
    val minAmount: BigDecimal,
    val withdrawMin: BigDecimal,
    val maxAmount: BigDecimal?,
)
