package io.horizontalsystems.bankwallet.modules.swap.model

import java.math.BigDecimal

data class PriceImpact(
        val value: BigDecimal,
        val level: Level
) {
    enum class Level {
        Normal, Warning, Forbidden
    }
}
