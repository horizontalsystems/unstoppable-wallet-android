package io.horizontalsystems.bankwallet.modules.settings.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage

class SubscriptionViewModel(
    private val localStorage: ILocalStorage
) : ViewModel() {


}


object SubscriptionModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SubscriptionViewModel(App.localStorage) as T
        }
    }
}