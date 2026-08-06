package io.horizontalsystems.walletkit.modules.solananetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.chain.solana.SolanaChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.App

object SolanaNetworkModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = SolanaNetworkService(
                (ChainRegistry[BlockchainType.Solana] as SolanaChainPlugin).rpcSourceManager
            )

            return SolanaNetworkViewModel(service) as T
        }
    }

}
