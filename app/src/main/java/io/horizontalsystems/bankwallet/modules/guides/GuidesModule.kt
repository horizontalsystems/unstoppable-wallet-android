package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.GuidesManager

object GuidesModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val guidesService = GuidesService(GuidesManager, App.connectivityManager)

            return GuidesViewModel(guidesService) as T
        }
    }
}
