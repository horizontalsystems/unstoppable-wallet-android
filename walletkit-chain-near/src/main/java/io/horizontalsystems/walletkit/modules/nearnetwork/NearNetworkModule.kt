package io.horizontalsystems.walletkit.modules.nearnetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.chain.near.NearChainPlugin
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType

object NearNetworkModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = NearNetworkService(
                requireNotNull(ChainRegistry[BlockchainType.Near] as? NearChainPlugin) {
                    "NEAR plugin is not registered"
                }.rpcSourceManager
            )

            return NearNetworkViewModel(service) as T
        }
    }

}
