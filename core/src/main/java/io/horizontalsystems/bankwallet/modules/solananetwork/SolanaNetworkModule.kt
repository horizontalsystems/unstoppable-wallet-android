package io.horizontalsystems.bankwallet.modules.solananetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object SolanaNetworkModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = SolanaNetworkService(
                App.solanaRpcSourceManager
            )

            return SolanaNetworkViewModel(service) as T
        }
    }

}
