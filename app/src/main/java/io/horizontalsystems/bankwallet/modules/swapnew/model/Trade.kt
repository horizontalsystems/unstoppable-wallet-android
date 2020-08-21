package io.horizontalsystems.bankwallet.modules.swapnew.model

import io.horizontalsystems.bankwallet.entities.Coin
import java.math.BigDecimal

data class Trade(
        val coinSending: Coin,
        val coinReceiving: Coin,
        val amountType: AmountType,
        val amountSending: BigDecimal?,
        val amountReceiving: BigDecimal?,
        val executionPrice: BigDecimal?,
        val priceImpact: PriceImpact?,
        val minMaxAmount: BigDecimal?
)
