package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.uniswapkit.models.TradeData
import java.math.BigDecimal

class UniversalSwapTradeData(
    val amountIn: BigDecimal?,
    val amountOut: BigDecimal?,
    val executionPrice: BigDecimal?,
    val priceImpact: BigDecimal?,
    private val tradeDataV2: TradeData?
) {

    fun getTradeDataV2(): TradeData {
        return tradeDataV2!!
    }

    companion object {
        fun buildFromTradeDataV2(tradeData: TradeData): UniversalSwapTradeData {
            return UniversalSwapTradeData(
                amountIn = tradeData.amountIn,
                amountOut = tradeData.amountOut,
                executionPrice = tradeData.executionPrice,
                priceImpact = tradeData.priceImpact,
                tradeDataV2 = tradeData
            )
        }
    }
}