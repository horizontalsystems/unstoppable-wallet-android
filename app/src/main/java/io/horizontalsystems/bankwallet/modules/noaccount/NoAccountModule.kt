package io.horizontalsystems.bankwallet.modules.noaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*


object NoAccountModule {

    interface INoAccountService{
        fun createAccount(predefinedAccountType: PredefinedAccountType): Account
        fun save(account: Account)
        fun derivationSetting(coinType: CoinType): DerivationSetting?
        fun resetDerivationSettings()
    }

    class Factory(private val coin: Coin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = NoAccountService(App.accountManager, App.accountCreator, App.derivationSettingsManager)
            return NoAccountViewModel(coin, service) as T
        }
    }
}
