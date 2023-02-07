package cash.p.terminal.modules.coin.investments

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.DataState
import io.horizontalsystems.marketkit.models.CoinInvestment
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class CoinInvestmentsService(
    private val coinUid: String,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager
) {
    private var disposable: Disposable? = null

    private val stateSubject = BehaviorSubject.create<DataState<List<CoinInvestment>>>()
    val stateObservable: Observable<DataState<List<CoinInvestment>>>
        get() = stateSubject

    val usdCurrency: Currency
        get() {
            val currencies = currencyManager.currencies
            return currencies.first { it.code == "USD" }
        }

    private fun fetch() {
        disposable?.dispose()

        marketKit.investmentsSingle(coinUid)
            .subscribeIO({ coinInvestments ->
                stateSubject.onNext(DataState.Success(coinInvestments))
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
