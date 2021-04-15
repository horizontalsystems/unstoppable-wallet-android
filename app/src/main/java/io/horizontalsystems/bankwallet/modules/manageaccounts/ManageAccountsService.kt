package io.horizontalsystems.bankwallet.modules.manageaccounts

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageAccountsService(
        private val accountManager: IAccountManager
) : Clearable {
    private val disposable = CompositeDisposable()

    private val itemsSubject = PublishSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsSubject.onNext(value)
        }
    val itemsObservable: Flowable<List<Item>> = itemsSubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        accountManager.accountsFlowable
                .subscribeIO { syncItems() }
                .let { disposable.add(it) }

        accountManager.activeAccountObservable
                .subscribeIO { syncItems() }
                .let { disposable.add(it) }

        syncItems()
    }

    private fun syncItems() {
        val activeAccount = accountManager.activeAccount
        items = accountManager.accounts.map { account ->
            Item(account, account == activeAccount)
        }
    }

    fun setActiveAccountId(activeAccountId: String) {
        accountManager.setActiveAccountId(activeAccountId)
    }

    override fun clear() {
        disposable.clear()
    }

    data class Item(
            val account: Account,
            val isActive: Boolean
    )

}
