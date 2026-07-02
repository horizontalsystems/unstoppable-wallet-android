package io.horizontalsystems.core.modules.zcashnetwork.addendpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.core.App

object AddZcashEndpointModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddZcashEndpointViewModel(App.zcashEndpointManager) as T
        }
    }
}
