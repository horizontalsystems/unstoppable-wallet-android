package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * A provider the app no longer quotes, kept resolvable by id for the swaps already made with
 * it: history needs its type and whether its swaps were a single transaction to lay the record
 * out, and tracking keys on the id. It supports no pair and can never be quoted.
 */
class RetiredSwapProvider(
    override val id: String,
    override val title: String,
    override val type: SwapProviderType,
    private val singleTransactionSwap: Boolean,
) : IMultiSwapProvider {
    override val requireTerms = false
    override val riskLevel = RiskLevel.EXCELLENT

    override fun isSingleTransactionSwap(tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String) = singleTransactionSwap

    override fun supports(blockchainType: BlockchainType) = false

    override fun supports(tokenFrom: Token, tokenTo: Token) = false

    override suspend fun fetchQuote(tokenIn: Token, tokenOut: Token, amountIn: BigDecimal): SwapQuote =
        throw UnsupportedOperationException("$title is no longer offered for swapping")

    override suspend fun fetchFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        recipient: Address?,
        slippage: BigDecimal,
    ): SwapFinalQuote = throw UnsupportedOperationException("$title is no longer offered for swapping")
}
