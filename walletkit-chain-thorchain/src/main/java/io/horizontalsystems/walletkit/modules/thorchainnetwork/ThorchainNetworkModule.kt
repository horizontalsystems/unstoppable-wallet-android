package io.horizontalsystems.walletkit.modules.thorchainnetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.chain.thorchain.ThorchainChainPlugin
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType

object ThorchainNetworkModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = ThorchainNetworkService(
                (ChainRegistry[BlockchainType.Thorchain] as ThorchainChainPlugin).rpcSourceManager
            )

            return ThorchainNetworkViewModel(service) as T
        }
    }

}
