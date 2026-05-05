package io.horizontalsystems.bankwallet.modules.zanonetwork

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object ZanoNetworkModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZanoNetworkViewModel(App.zanoNodeManager) as T
        }
    }

}
