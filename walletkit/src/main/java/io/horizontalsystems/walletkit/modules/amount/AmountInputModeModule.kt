package io.horizontalsystems.walletkit.modules.amount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.xrate.XRateService

object AmountInputModeModule {

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

            return AmountInputModeViewModel(App.localStorage, xRateService, coinUid) as T
        }
    }
}