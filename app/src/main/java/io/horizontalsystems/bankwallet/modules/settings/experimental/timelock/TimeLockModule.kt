package io.horizontalsystems.bankwallet.modules.settings.experimental.timelock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object TimeLockModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimeLockViewModel(App.localStorage) as T
        }
    }
}
