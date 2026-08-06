package io.horizontalsystems.walletkit.modules.zcashnetwork.addendpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App

object AddZcashEndpointModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddZcashEndpointViewModel(App.zcashEndpointManager) as T
        }
    }
}
