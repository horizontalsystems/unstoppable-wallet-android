package cash.p.terminal.modules.rooteddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object RootedDeviceModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = RootedDeviceViewModel(App.localStorage)
            return viewModel as T
        }
    }
}
