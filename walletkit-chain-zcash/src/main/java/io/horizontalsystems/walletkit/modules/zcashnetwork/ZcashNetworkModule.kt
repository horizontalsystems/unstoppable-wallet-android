package io.horizontalsystems.walletkit.modules.zcashnetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App

object ZcashNetworkModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZcashNetworkViewModel(App.zcashEndpointManager) as T
        }
    }

}
