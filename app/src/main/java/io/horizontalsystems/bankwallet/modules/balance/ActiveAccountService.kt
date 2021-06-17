package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class ActiveAccountService(private val accountManager: IAccountManager) : Clearable {
    private val disposables = CompositeDisposable()

    private val activeAccountSubject = BehaviorSubject.create<Account>()
    val activeAccountObservable: Flowable<Account> = activeAccountSubject.toFlowable(BackpressureStrategy.DROP)

    init {
        refreshActiveAccount()

        accountManager.activeAccountObservable
            .subscribeIO {
                refreshActiveAccount()
            }
            .let {
                disposables.add(it)
            }

        accountManager.accountsFlowable
            .subscribeIO {
                refreshActiveAccount()
            }
            .let {
                disposables.add(it)
            }

    }

    private fun refreshActiveAccount() {
        accountManager.activeAccount?.let {
            activeAccountSubject.onNext(it)
        }
    }

    override fun clear() {
        disposables.clear()
    }

}
