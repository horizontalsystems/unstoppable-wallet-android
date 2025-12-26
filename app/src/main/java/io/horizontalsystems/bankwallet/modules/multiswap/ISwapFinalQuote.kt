package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

interface ISwapFinalQuote {
    val tokenIn: Token
    val tokenOut: Token
    val amountIn: BigDecimal
    val amountOut: BigDecimal
    val amountOutMin: BigDecimal?
    val sendTransactionData: SendTransactionData
    val priceImpact: BigDecimal?
    val fields: List<DataField>
    val cautions: List<HSCaution>
}

data class SwapFinalQuoteEvm(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val amountOutMin: BigDecimal?,
    override val sendTransactionData: SendTransactionData.Evm,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>,
    override val cautions: List<HSCaution> = listOf()
) : ISwapFinalQuote

data class SwapFinalQuoteThorChain(
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val amountOut: BigDecimal,
    override val amountOutMin: BigDecimal?,
    override val sendTransactionData: SendTransactionData,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>,
    override val cautions: MutableList<HSCaution>
) : ISwapFinalQuote
