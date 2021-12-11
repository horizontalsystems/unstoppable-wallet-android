package io.horizontalsystems.bankwallet.modules.coin.reports

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinReport
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class CoinReportsService(
    private val coinUid: String,
    private val marketKit: MarketKit
) {
    private var disposable: Disposable? = null

    private val stateSubject = BehaviorSubject.create<DataState<List<CoinReport>>>()
    val stateObservable: Observable<DataState<List<CoinReport>>>
        get() = stateSubject

    private fun fetch() {
        disposable?.dispose()

        marketKit.coinReportsSingle(coinUid)
            .doOnSubscribe { stateSubject.onNext(DataState.Loading) }
            .subscribeIO({ reports ->
                stateSubject.onNext(DataState.Success(reports))
            }, { error ->
                stateSubject.onNext(DataState.Error(error))
            }).let { disposable = it }
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposable?.dispose()
    }
}
