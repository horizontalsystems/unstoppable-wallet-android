package io.horizontalsystems.bankwallet.modules.swap.coinselect

import io.horizontalsystems.bankwallet.entities.Coin
import java.math.BigDecimal

data class SwapCoinItem(val coin: Coin, val balance: BigDecimal?)
