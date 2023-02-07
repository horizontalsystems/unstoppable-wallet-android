package cash.p.terminal.modules.coin.majorholders

import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import io.horizontalsystems.marketkit.models.TokenHolder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinMajorHoldersService(
    private val coinUid: String,
    private val marketKit: MarketKitWrapper
) {
    private val disposables = CompositeDisposable()

    private val stateSubject = BehaviorSubject.create<DataState<List<TokenHolder>>>()
    val stateObservable: Observable<DataState<List<TokenHolder>>>
        get() = stateSubject

    private fun fetch() {
        marketKit.topHoldersSingle(coinUid)
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
