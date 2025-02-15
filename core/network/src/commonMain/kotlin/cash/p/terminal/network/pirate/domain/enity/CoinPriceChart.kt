package cash.p.terminal.network.pirate.domain.enity

import java.math.BigDecimal

data class CoinPriceChart(
    val timestamp: Long,
    val price: BigDecimal
)
