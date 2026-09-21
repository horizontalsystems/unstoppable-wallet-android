package io.horizontalsystems.walletkit.modules.xrpnetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.chain.xrp.XrpChainPlugin
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType

object XrpNetworkModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = XrpNetworkService(
                requireNotNull(ChainRegistry[BlockchainType.Xrp] as? XrpChainPlugin) {
                    "XRP plugin is not registered"
                }.rpcSourceManager
            )

            return XrpNetworkViewModel(service) as T
        }
    }

}
