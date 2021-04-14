package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.reactivex.disposables.CompositeDisposable

class ManageAccountsViewModel(
        private val service: ManageAccountsService
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val viewItemsLiveData = MutableLiveData<List<AccountViewItem>>()

    init {
        service.accountsObservable
                .subscribeIO { sync(it) }
                .let { disposable.add(it) }

        sync(service.accounts)
    }

    private fun sync(accounts: List<Account>) {
        viewItemsLiveData.postValue(accounts.map { getViewItem(it, false) })
    }

    private fun getViewItem(account: Account, selected: Boolean): AccountViewItem {
        return AccountViewItem(account.id, account.name, getDescription(account.type), selected, !account.isBackedUp)
    }

    private fun getDescription(accountType: AccountType): String {
        return when (accountType) {
            is AccountType.Mnemonic -> {
                val count = accountType.words.size
                "$count words ${if (accountType.salt != null) "with passphrase" else ""}"
            }
            else -> ""
        }
    }

    fun onSelect(accountViewItem: AccountViewItem) {

        viewItemsLiveData.postValue(service.accounts.map { getViewItem(it, it.id == accountViewItem.accountId) })
    }

}
