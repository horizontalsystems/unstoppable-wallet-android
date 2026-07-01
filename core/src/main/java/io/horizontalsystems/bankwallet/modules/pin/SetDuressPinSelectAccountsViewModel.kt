package io.horizontalsystems.bankwallet.modules.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account

class SetDuressPinSelectAccountsViewModel(accountManager: IAccountManager) : ViewModel() {

    val watchAccounts: List<Account>
    val regularAccounts: List<Account>

    init {
        val (watch, regular) = accountManager.accounts.partition { it.isWatchAccount }
        watchAccounts = watch
        regularAccounts = regular
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetDuressPinSelectAccountsViewModel(App.accountManager) as T
        }
    }
}
