package cash.p.terminal.entities.transactionrecords

import cash.p.terminal.wallet.Token
import java.math.BigDecimal

data class ShortOutgoingTransactionRecord(
    val token: Token?,
    val amountOut: BigDecimal?,
    val timestamp: Long
)
