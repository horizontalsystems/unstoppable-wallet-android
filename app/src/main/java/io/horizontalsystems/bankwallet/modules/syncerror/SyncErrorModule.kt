package io.horizontalsystems.bankwallet.modules.syncerror

import io.horizontalsystems.marketkit.models.Blockchain

object SyncErrorModule {

    sealed class BlockchainWrapper {
        data class Bitcoin(val blockchain: Blockchain) : BlockchainWrapper()
        data class Evm(val blockchain: Blockchain) : BlockchainWrapper()
        object Monero : BlockchainWrapper()
        object Zano : BlockchainWrapper()
        object Zcash : BlockchainWrapper()
    }
}
