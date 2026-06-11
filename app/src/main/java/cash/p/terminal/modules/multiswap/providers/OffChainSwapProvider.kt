package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult

interface OffChainSwapProvider : IMultiSwapProvider {
    fun onTransactionCompleted(
        transaction: SwapProviderTransaction,
        result: SendTransactionResult,
    )
}

val IMultiSwapProvider.isOffChain: Boolean
    get() = this is OffChainSwapProvider
