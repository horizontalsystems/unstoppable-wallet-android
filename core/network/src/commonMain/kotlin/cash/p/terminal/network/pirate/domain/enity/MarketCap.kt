package cash.p.terminal.network.pirate.domain.enity

import java.math.BigDecimal

data class MarketCap(
    val value24h: Map<String, BigDecimal>,
)
