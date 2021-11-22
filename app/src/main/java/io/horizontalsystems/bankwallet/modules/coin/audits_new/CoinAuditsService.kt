package io.horizontalsystems.bankwallet.modules.coin.audits_new

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Auditor
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class CoinAuditsService(
    private val addresses: List<String>,
    private val marketKit: MarketKit
) {
    private var disposable: Disposable? = null

    private val stateSubject = BehaviorSubject.create<DataState<List<Auditor>>>()
    val stateObservable: Observable<DataState<List<Auditor>>>
        get() = stateSubject

    private fun fetch() {
        disposable?.dispose()

        marketKit.auditReportsSingle(addresses)
            .doOnSubscribe { stateSubject.onNext(DataState.Loading) }
            .subscribeIO({ auditors ->
                stateSubject.onNext(DataState.Success(auditors))
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
