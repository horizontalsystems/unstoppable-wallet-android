package cash.p.terminal.modules.multiswap

import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.CurrencyValue
import java.math.BigDecimal

data class CoinBalanceItem(
    val token: Token,
    val balance: BigDecimal?,
    val fiatBalanceValue: CurrencyValue?,
)
