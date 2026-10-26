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
    // The transaction to estimate and sign, or null when nothing can be built before the order
    // exists: a transfer provider's deposit address arrives with the commit, and the server named
    // no stand-in to price the transfer against. Such a quote is shown without a network fee.
    val sendTransactionData: SendTransactionData?,
    val priceImpact: BigDecimal?,
    val fields: List<DataField>,
    val estimatedTime: Long?,
    val slippage: BigDecimal?,
    val providerSwapId: String? = null,
    val fromAsset: String? = null,
    val toAsset: String? = null,
    val depositAddress: String? = null,
    // Memo the deposit transfer must carry, when the route demands one — vehicles
    // with no memo channel must decline such routes instead of dropping it.
    val depositMemo: String? = null,
    // Spender of the committed route's approval, when the provider reports one —
    // it can differ from the transaction's `to` (routers vs. dedicated spenders).
    val approvalSpender: String? = null,
    // Provider-private state carried from the preview to [IMultiSwapProvider.commit], such as the
    // token that turns the previewed route into an order. Opaque to everything else.
    val providerContext: Any? = null,
    // Whether this route can be broadcast through the MEV-protected RPC: a property of the
    // quote, decided by the provider from the route, never of the provider. The confirmation
    // offers the toggle only when it is true and the sell chain has a protected sender.
    val mevProtectionAllowed: Boolean = false,
    // A split swap's outputs in request order; null for a single-output swap. [amountOut] is
    // their total. After the commit each carries the uuid of its own swap record.
    val splitOutputs: List<SplitSwapOutput>? = null,
)
