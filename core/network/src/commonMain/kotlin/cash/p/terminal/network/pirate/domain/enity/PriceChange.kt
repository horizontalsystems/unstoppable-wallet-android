package cash.p.terminal.network.pirate.domain.enity

import java.math.BigDecimal

data class PriceChange(
    val percentage1h: Map<String, BigDecimal>,
    val percentage24h: Map<String, BigDecimal>,
    val percentage7d: Map<String, BigDecimal>,
    val percentage30d: Map<String, BigDecimal>,
    val percentage1y: Map<String, BigDecimal>
)
