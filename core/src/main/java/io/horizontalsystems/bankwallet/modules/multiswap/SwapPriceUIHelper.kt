package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode

class SwapPriceUIHelper(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal, amountOut: BigDecimal) {
    private val price = amountOut.divide(amountIn, tokenOut.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
    private val priceInv = amountIn.divide(amountOut, tokenIn.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()

    val priceStr = "${CoinValue(tokenIn, BigDecimal.ONE).getFormattedFull()} = ${CoinValue(tokenOut, price).getFormattedFull()}"
    val priceInvStr = "${CoinValue(tokenOut, BigDecimal.ONE).getFormattedFull()} = ${CoinValue(tokenIn, priceInv).getFormattedFull()}"
}
