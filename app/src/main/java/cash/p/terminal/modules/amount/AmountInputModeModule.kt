package cash.p.terminal.modules.amount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.xrate.XRateService

object AmountInputModeModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

            return AmountInputModeViewModel(App.localStorage, xRateService, wallet.coin.uid) as T
        }
    }
}