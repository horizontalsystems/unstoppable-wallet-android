package com.quantum.wallet.bankwallet.modules.watchaddress.selectblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.AccountType
import com.quantum.wallet.bankwallet.modules.watchaddress.WatchAddressService

object SelectBlockchainsModule {
    class Factory(val accountType: AccountType, val accountName: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WatchAddressService(
                App.accountManager,
                App.walletActivator,
                App.accountFactory,
                App.marketKit,
                App.evmBlockchainManager,
                App.restoreSettingsManager
            )
            return SelectBlockchainsViewModel(accountType, accountName, service) as T
        }
    }
}
