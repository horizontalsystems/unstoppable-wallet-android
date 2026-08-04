package io.horizontalsystems.walletkit.core.adapters

import java.math.BigDecimal

fun Long.scaledDown(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
}
