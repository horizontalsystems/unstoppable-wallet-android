package cash.p.terminal.wallet.models

import cash.p.terminal.wallet.Token
import java.math.BigDecimal

data class NftPrice(
    val token: Token,
    val value: BigDecimal
)
