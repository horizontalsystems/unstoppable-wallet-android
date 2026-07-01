package io.horizontalsystems.bankwallet.modules.rooteddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.RootUtil

object RootedDeviceModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = RootedDeviceViewModel(App.localStorage, RootUtil)
            return viewModel as T
        }
    }
}
