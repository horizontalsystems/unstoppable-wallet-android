package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class CoinBalanceItem(
    val token: Token,
    val balance: BigDecimal?,
    val fiatBalanceValue: CurrencyValue?,
)
