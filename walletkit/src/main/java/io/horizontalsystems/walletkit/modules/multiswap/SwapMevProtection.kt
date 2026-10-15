package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI

/**
 * When a swap may be broadcast through the MEV-protected RPC. The private RPC protects one
 * transaction on one EVM chain, so the pair must stay on that chain; whether the chain has a
 * protected sender at all is the send service's call ([SwapFinalQuote.mevProtectionAllowed]
 * is combined with it on the confirmation screen).
 */
object SwapMevProtection {
    /** A transaction the app signs itself on a same-chain EVM pair. */
    fun allowedForPair(tokenIn: Token, tokenOut: Token): Boolean =
        tokenIn.blockchainType == tokenOut.blockchainType && tokenIn.blockchainType.isEvm

    /**
     * A USwap route: only a server-built signed transaction qualifies; a deposit transfer or a
     * vault deposit reaches a different contract and is never a candidate.
     */
    fun allowedForRoute(execution: UnstoppableAPI.Response.Execution?, tokenIn: Token, tokenOut: Token): Boolean =
        execution?.method == "signed_transaction" && allowedForPair(tokenIn, tokenOut)
}
