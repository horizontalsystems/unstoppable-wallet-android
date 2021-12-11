package io.horizontalsystems.bankwallet.modules.coin.majorholders

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.TokenHolder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinMajorHoldersService(
    private val coinUid: String,
    private val marketKit: MarketKit
) {
    private val disposables = CompositeDisposable()

    private val stateSubject = BehaviorSubject.create<DataState<List<TokenHolder>>>()
    val stateObservable: Observable<DataState<List<TokenHolder>>>
        get() = stateSubject

    private fun fetch() {
        marketKit.topHoldersSingle(coinUid)
            .doOnSubscribe { stateSubject.onNext(DataState.Loading) }
            .subscribeIO({
                stateSubject.onNext(DataState.Success(it))
            }, {
                stateSubject.onNext(DataState.Error(it))
            }).let { disposables.add(it) }
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposables.clear()
    }
}
