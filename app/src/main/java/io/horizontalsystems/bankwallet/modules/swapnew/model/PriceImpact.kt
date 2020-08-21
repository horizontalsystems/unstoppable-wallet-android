package io.horizontalsystems.bankwallet.modules.swapnew.model

import java.math.BigDecimal

data class PriceImpact(
        val value: BigDecimal,
        val level: Level
) {
    enum class Level {
        Normal, Warning, Forbidden
    }
}
