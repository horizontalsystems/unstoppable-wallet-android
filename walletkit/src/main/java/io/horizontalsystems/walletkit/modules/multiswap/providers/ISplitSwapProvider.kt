package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.modules.multiswap.SplitSwapRecipient
import io.horizontalsystems.walletkit.modules.multiswap.SwapFinalQuote
import io.horizontalsystems.walletkit.modules.multiswap.SwapQuote
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionSettings
import java.math.BigDecimal

/**
 * A provider that splits one deposit between several outputs of the same buy token, each paid
 * to its own address. The quote and the final quote are the single-swap ones with the buy side
 * per output: the quote's `amountOut` is the outputs' total, and the final quote carries
 * [SwapFinalQuote.splitOutputs]. [IMultiSwapProvider.commit] creates the order for a split
 * preview exactly as for a single one; the committed quote's `providerSwapId` is the split's id.
 */
interface ISplitSwapProvider : IMultiSwapProvider {
    /** Prices a split with the given shares of the deposit, which must sum to 100. */
    suspend fun fetchSplitQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        percents: List<Int>,
    ): SwapQuote

    /** The confirmation screen's split quote, with a destination per output. No side effects. */
    suspend fun fetchSplitFinalQuote(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        recipients: List<SplitSwapRecipient>,
        sendTransactionSettings: SendTransactionSettings?,
        swapQuote: SwapQuote,
        slippage: BigDecimal,
    ): SwapFinalQuote

    /** Reports the broadcast deposit hash to every output of the split. Best-effort. */
    suspend fun reportSplitInboundTxHash(splitUuid: String, txHash: String)
}
