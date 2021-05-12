package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class ManageAccountsViewModel(
        private val service: ManageAccountsService,
        private val mode: ManageAccountsModule.Mode,
        private val clearables: List<Clearable>
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val viewItemsLiveData = MutableLiveData<List<AccountViewItem>>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val isCloseButtonVisible: Boolean = mode == ManageAccountsModule.Mode.Switcher

    init {
        service.itemsObservable
                .subscribeIO { sync(it) }
                .let { disposable.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageAccountsService.Item>) {
        val sortedItems = items.sortedBy { it.account.name.toLowerCase(Locale.ENGLISH) }
        viewItemsLiveData.postValue(sortedItems.map { getViewItem(it) })
    }

    private fun getViewItem(item: ManageAccountsService.Item): AccountViewItem {
        val account = item.account
        return AccountViewItem(account.id, account.name, getDescription(account.type), item.isActive, !account.isBackedUp)
    }

    private fun getDescription(accountType: AccountType): String {
        return when (accountType) {
            is AccountType.Mnemonic -> {
                val count = accountType.words.size

                if (accountType.passphrase.isNotBlank()) {
                    Translator.getString(R.string.ManageAccount_NWordsWithPassphrase, count)
                } else {
                    Translator.getString(R.string.ManageAccount_NWords, count)
                }
            }
            else -> ""
        }
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
