package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class ManageAccountsViewModel(
        private val service: ManageAccountsService,
        private val mode: ManageAccountsModule.Mode,
        private val clearables: List<Clearable>
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val viewItemsLiveData = MutableLiveData<Pair<List<AccountViewItem>, List<AccountViewItem>>>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val isCloseButtonVisible: Boolean = mode == ManageAccountsModule.Mode.Switcher

    init {
        service.itemsObservable
                .subscribeIO { sync(it) }
                .let { disposable.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageAccountsService.Item>) {
        val sortedItems = items.sortedBy { it.account.name.lowercase() }
        val (watchAccounts, regularAccounts) = sortedItems.partition {
            it.account.isWatchAccount
        }
        viewItemsLiveData.postValue(Pair(regularAccounts.map { getViewItem(it) }, watchAccounts.map { getViewItem(it) }))
    }

    private fun getViewItem(item: ManageAccountsService.Item): AccountViewItem {
        val account = item.account
        return AccountViewItem(account.id, account.name, account.type.description, item.isActive, !account.isBackedUp, account.isWatchAccount)
    }

    fun onSelect(accountViewItem: AccountViewItem) {
        service.setActiveAccountId(accountViewItem.accountId)

        if (mode == ManageAccountsModule.Mode.Switcher) {
            finishLiveEvent.postValue(Unit)
        }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

}
