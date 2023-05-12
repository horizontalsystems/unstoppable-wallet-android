package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import java.math.BigDecimal

class UniversalSwapTradeData(
    val amountIn: BigDecimal?,
    val amountOut: BigDecimal?,
    val executionPrice: BigDecimal?,
    val priceImpact: BigDecimal?,
    private val tradeDataV2: TradeData? = null,
    private val tradeDataV3: TradeDataV3? = null,
) {

    fun getTradeDataV2(): TradeData {
        return tradeDataV2!!
    }

    fun getTradeDataV3(): TradeDataV3 {
        return tradeDataV3!!
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
        fun buildFromTradeDataV3(tradeDataV3: TradeDataV3): UniversalSwapTradeData {
            return UniversalSwapTradeData(
                amountIn = tradeDataV3.tokenAmountIn.decimalAmount,
                amountOut = tradeDataV3.tokenAmountOut.decimalAmount,
                executionPrice = tradeDataV3.executionPrice,
                priceImpact = tradeDataV3.priceImpact,
                tradeDataV3 = tradeDataV3
            )
        }
    }
}