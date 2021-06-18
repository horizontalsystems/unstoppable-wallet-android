package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BalanceConfigurator(private val localStorage: ILocalStorage) {

    private val sortTypeSubject = PublishSubject.create<Unit>()
    val sortTypeObservable: Observable<Unit> = sortTypeSubject

    var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            sortTypeSubject.onNext(Unit)
        }

    var balanceHidden: Boolean
        get() = localStorage.balanceHidden
        set(value) {
            localStorage.balanceHidden = value
        }

}
