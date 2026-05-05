package io.horizontalsystems.bankwallet.modules.zanonetwork.addnode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object AddZanoNodeModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddZanoNodeViewModel(App.zanoNodeManager) as T
        }
    }
}
