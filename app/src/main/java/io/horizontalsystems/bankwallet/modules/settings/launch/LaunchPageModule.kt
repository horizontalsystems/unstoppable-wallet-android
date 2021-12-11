package io.horizontalsystems.bankwallet.modules.settings.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object LaunchPageModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = LaunchPageService(App.localStorage)
            return LaunchPageViewModel(service) as T
        }
    }

}
