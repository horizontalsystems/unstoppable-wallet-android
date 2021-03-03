package io.horizontalsystems.bankwallet.modules.noaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin

object NoAccountModule {

    interface INoAccountService{
        fun createAccount(predefinedAccountType: PredefinedAccountType): Account
        fun save(account: Account)
        fun createWallet(coin: Coin, account: Account)
        fun resetAddressFormatSettings()
    }

    class Factory(private val coin: Coin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = NoAccountService(App.accountManager, App.accountCreator, App.walletManager, App.derivationSettingsManager, App.bitcoinCashCoinTypeManager)
            return NoAccountViewModel(coin, service) as T
        }
    }
}
