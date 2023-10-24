package io.horizontalsystems.bankwallet.modules.hardwarewallet.selectblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.hardwarewallet.HardwareWalletService

object SelectHardwareBlockchainsModule {

    const val accountTypeKey = "accountTypeKey"
    const val accountNameKey = "accountNameKey"

    class Factory(val accountType: AccountType, val accountName: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = HardwareWalletService(App.accountManager, App.walletActivator, App.accountFactory, App.marketKit, App.evmBlockchainManager)
            return SelectHardwareBlockchainsViewModel(accountType, accountName, service) as T
        }
    }
}
