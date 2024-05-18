package cash.p.terminal.modules.coin.investments

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.DataState
import io.horizontalsystems.marketkit.models.CoinInvestment
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class CoinInvestmentsService(
    private val coinUid: String,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val stateSubject = BehaviorSubject.create<DataState<List<CoinInvestment>>>()
    val stateObservable: Observable<DataState<List<CoinInvestment>>>
        get() = stateSubject

    val usdCurrency: Currency
        get() {
            val currencies = currencyManager.currencies
            return currencies.first { it.code == "USD" }
        }

    private fun fetch() {
        coroutineScope.launch {
            try {
                val coinInvestments = marketKit.investmentsSingle(coinUid).await()
                stateSubject.onNext(DataState.Success(coinInvestments))
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
