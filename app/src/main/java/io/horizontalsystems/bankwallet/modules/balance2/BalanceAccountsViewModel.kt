package io.horizontalsystems.bankwallet.modules.balance2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.balance.AccountViewItem
import io.reactivex.disposables.CompositeDisposable

class BalanceAccountsViewModel(private val accountManager: IAccountManager) : ViewModel() {
    private val disposables = CompositeDisposable()

    var emptyAccounts by mutableStateOf<Boolean?>(null)
        private set
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

        handleAccounts(accountManager.accounts)
        accountManager.accountsFlowable
            .subscribeIO {
                handleAccounts(it)
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

            AccountViewItem(address, account.type !is AccountType.Address, account.type is AccountType.Address, account.name)
        }
    }

    private fun handleAccounts(accounts: List<Account>) {
        emptyAccounts = accounts.isEmpty()
    }

    override fun onCleared() {
        disposables.clear()
    }
}