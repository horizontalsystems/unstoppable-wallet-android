package io.horizontalsystems.walletkit.modules.zanonetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App

object ZanoNetworkModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZanoNetworkViewModel(App.zanoNodeManager) as T
        }
    }

}
