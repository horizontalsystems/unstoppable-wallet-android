package io.horizontalsystems.walletkit.modules.thorchainnetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App

object ThorchainNetworkModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = ThorchainNetworkService(
                App.thorchainRpcSourceManager
            )

            return ThorchainNetworkViewModel(service) as T
        }
    }

}
