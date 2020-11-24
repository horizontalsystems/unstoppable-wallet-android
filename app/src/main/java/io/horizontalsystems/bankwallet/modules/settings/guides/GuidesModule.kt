package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.GuidesManager

object GuidesModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val guidesService = GuidesRepository(GuidesManager, App.connectivityManager, App.languageManager)

            return GuidesViewModel(guidesService) as T
        }
    }
}

sealed class DataState<T> {
    class Loading<T> : DataState<T>()
    class Success<T>(val data: T) : DataState<T>()
    class Error<T>(val throwable: Throwable) : DataState<T>()
}
