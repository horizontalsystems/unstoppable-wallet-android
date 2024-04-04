package io.horizontalsystems.bankwallet.modules.coin.reports

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.CoinReport
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class CoinReportsService(
    private val coinUid: String,
    private val marketKit: MarketKitWrapper
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val stateSubject = BehaviorSubject.create<DataState<List<CoinReport>>>()
    val stateObservable: Observable<DataState<List<CoinReport>>>
        get() = stateSubject

    private fun fetch() {
        coroutineScope.launch {
            try {
                val reports = marketKit.coinReportsSingle(coinUid).await()
                stateSubject.onNext(DataState.Success(reports))
            } catch (e: Throwable) {
                stateSubject.onNext(DataState.Error(e))
            }
        }
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        coroutineScope.cancel()
    }
}
