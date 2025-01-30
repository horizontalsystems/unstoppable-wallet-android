package cash.p.terminal.wallet.entities

import java.math.BigDecimal

data class BalanceData(
    val available: BigDecimal,
    val timeLocked: BigDecimal = BigDecimal.ZERO,
    val notRelayed: BigDecimal = BigDecimal.ZERO,
    val pending: BigDecimal = BigDecimal.ZERO,
    val stackingUnpaid: BigDecimal = BigDecimal.ZERO,
) {
    val total get() = available + timeLocked + notRelayed + pending
}