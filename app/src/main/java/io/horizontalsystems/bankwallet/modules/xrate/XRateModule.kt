package io.horizontalsystems.bankwallet.modules.xrate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object XRateModule {

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return XRateViewModel(coinUid, App.marketKit, App.currencyManager.baseCurrency) as T
        }

    }

}
