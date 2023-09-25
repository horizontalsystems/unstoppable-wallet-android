package io.horizontalsystems.bankwallet.modules.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager

class SetDuressPinSelectAccountsViewModel(accountManager: IAccountManager) : ViewModel() {
    val items = accountManager.accounts

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetDuressPinSelectAccountsViewModel(App.accountManager) as T
        }
    }
}
