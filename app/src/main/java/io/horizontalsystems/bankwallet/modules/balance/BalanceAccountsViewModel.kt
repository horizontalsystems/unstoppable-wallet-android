package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.reactivex.disposables.CompositeDisposable

class BalanceAccountsViewModel(private val accountManager: IAccountManager) : ViewModel() {
    private val disposables = CompositeDisposable()

    var accountViewItem by mutableStateOf<AccountViewItem?>(null)
        private set

    init {
        handleAccount(accountManager.activeAccount)
        accountManager.activeAccountObservable
            .subscribeIO {
                handleAccount(it.orElse(null))
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleAccount(activeAccount: Account?) {
        accountViewItem = activeAccount?.let { account ->

            val address = when (account.type) {
                is AccountType.Address -> account.type.address
                else -> null
            }

            AccountViewItem(address, account.type !is AccountType.Address, account.type is AccountType.Address, account.name, account.id)
        }
    }

    override fun onCleared() {
        disposables.clear()
    }
}

data class AccountViewItem(val address: String?, val manageCoinsAllowed: Boolean, val isWatchAccount: Boolean, val name: String = "", val id: String)