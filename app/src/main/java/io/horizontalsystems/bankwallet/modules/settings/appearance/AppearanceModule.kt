package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object AppearanceModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = LaunchScreenService(App.localStorage)
            return AppearanceViewModel(service) as T
        }
    }

}
