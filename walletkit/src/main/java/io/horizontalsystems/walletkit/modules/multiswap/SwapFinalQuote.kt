package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SwapFinalQuote(
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val amountOut: BigDecimal,
    val amountOutMin: BigDecimal?,
    val sendTransactionData: SendTransactionData,
    val priceImpact: BigDecimal?,
    val fields: List<DataField>,
    val estimatedTime: Long?,
    val slippage: BigDecimal?,
    val providerSwapId: String? = null,
    val fromAsset: String? = null,
    val toAsset: String? = null,
    val depositAddress: String? = null,
)
