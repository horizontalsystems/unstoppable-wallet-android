package cash.p.terminal.modules.multiswap

import cash.p.terminal.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class CoinBalanceItem(
    val token: Token,
    val balance: BigDecimal?,
    val fiatBalanceValue: CurrencyValue?,
)
