package io.horizontalsystems.bankwallet.modules.zcashnetwork.addendpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object AddZcashEndpointModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddZcashEndpointViewModel(App.zcashEndpointManager) as T
        }
    }
}
