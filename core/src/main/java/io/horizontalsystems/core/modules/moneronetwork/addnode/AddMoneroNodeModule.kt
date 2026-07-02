package io.horizontalsystems.core.modules.moneronetwork.addnode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.core.App

object AddMoneroNodeModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddMoneroNodeViewModel(App.moneroNodeManager) as T
        }
    }
}
