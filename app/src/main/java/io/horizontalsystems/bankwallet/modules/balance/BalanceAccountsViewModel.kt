package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class BalanceAccountsViewModel(accountManager: IAccountManager) : ViewModel() {
    private val disposables = CompositeDisposable()

    var accountViewItem by mutableStateOf<AccountViewItem?>(null)
        private set

    init {
        handleAccount(accountManager.activeAccount)
        accountManager.activeAccountObservable
            .subscribeIO {
                viewModelScope.launch {
                    handleAccount(it.orElse(null))
                }
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleAccount(activeAccount: Account?) {
        accountViewItem = activeAccount?.let { account ->
            AccountViewItem(account.manageCoinsAllowed, account.isWatchAccount, account.name, account.id)
        }
    }

    override fun onCleared() {
        disposables.clear()
    }
}

data class AccountViewItem(val manageCoinsAllowed: Boolean, val isWatchAccount: Boolean, val name: String = "", val id: String)