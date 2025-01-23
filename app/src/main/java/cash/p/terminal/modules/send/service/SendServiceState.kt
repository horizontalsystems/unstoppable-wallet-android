package cash.p.terminal.modules.send.service

import cash.p.terminal.wallet.Token
import java.math.BigDecimal

data class SendServiceState(
    val feeToken: Token,
    val fee: BigDecimal
)