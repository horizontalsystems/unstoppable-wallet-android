package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.isEvm

/**
 * Drops the `/v3/rate` routes this app cannot execute, judged from the venue's `executionType`
 * (a rate route carries no `execution` yet): a vault deposit (`thorchain_deposit`) sold from a
 * chain whose deposit cannot carry the route memo, and `stellar_broker` routes, an interactive
 * WebSocket session this app does not run. A route naming no provider is dropped too: it has no
 * venue to preview against. An unknown execution type is kept; the preview decides.
 */
object USwapRouteFilter {
    const val VAULT_EXECUTION_TYPE = "thorchain_deposit"
    const val STELLAR_BROKER_EXECUTION_TYPE = "stellar_broker"

    fun filter(
        routes: List<UnstoppableAPI.Response.Route>,
        tokenIn: Token,
        executionType: (providerId: String) -> String?,
    ): List<UnstoppableAPI.Response.Route> = routes.filter { route ->
        val providerId = route.providerId
        !providerId.isNullOrEmpty() && isExecutable(executionType(providerId), tokenIn)
    }

    fun isExecutable(executionType: String?, tokenIn: Token): Boolean = when (executionType) {
        STELLAR_BROKER_EXECUTION_TYPE -> false
        VAULT_EXECUTION_TYPE -> supportsVaultDeposit(tokenIn.blockchainType)
        else -> true
    }

    /**
     * A vault deposit needs the swap memo to reach the vault. The app can carry it on chains
     * whose memo is public on-chain (UTXO OP_RETURN, THORChain / Maya memo, a TON comment, a
     * Stellar text memo), inside a server-built EVM transaction (calldata / router call), or
     * through a vault's shielded memo address on Zcash. An XRP payment carries a destination
     * tag, not a text memo, and Solana, Tron and the private chains carry none, so they are not
     * offered as a sell side even when the server quotes them.
     */
    fun supportsVaultDeposit(blockchainType: BlockchainType): Boolean =
        blockchainType.isEvm ||
            blockchainType == BlockchainType.Zcash ||
            (blockchainType != BlockchainType.Xrp && blockchainType.memoDelivery == MemoDelivery.OnChainPublic)
}
