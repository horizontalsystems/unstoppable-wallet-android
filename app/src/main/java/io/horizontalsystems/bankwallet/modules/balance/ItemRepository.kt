package io.horizontalsystems.bankwallet.modules.balance

import io.reactivex.Observable

interface ItemRepository<T> {
    fun refresh()

    val itemsObservable: Observable<List<T>>
}
