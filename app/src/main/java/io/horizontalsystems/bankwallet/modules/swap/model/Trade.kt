package io.horizontalsystems.bankwallet.modules.swap.model

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import java.math.BigDecimal

data class Trade(
        val coinSending: Coin,
        val coinReceiving: Coin,
        val amountType: AmountType,
        val amountSending: BigDecimal?,
        val amountReceiving: BigDecimal?,
        val executionPrice: BigDecimal?,
        val priceImpact: PriceImpact?,
        val swapFee: CoinValue?,
        val minMaxAmount: BigDecimal?
)
