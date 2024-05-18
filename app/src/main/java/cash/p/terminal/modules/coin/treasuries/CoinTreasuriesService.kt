package cash.p.terminal.modules.coin.treasuries

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.coin.treasuries.CoinTreasuriesModule.TreasuryTypeFilter
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinTreasury
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class CoinTreasuriesService(
    val coin: Coin,
    private val repository: CoinTreasuriesRepository,
    private val currencyManager: CurrencyManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val stateSubject = BehaviorSubject.create<DataState<List<CoinTreasury>>>()
    val stateObservable: Observable<DataState<List<CoinTreasury>>>
        get() = stateSubject

    val currency: Currency
        get() = currencyManager.baseCurrency

    val treasuryTypes = TreasuryTypeFilter.values().toList()
    var treasuryType: TreasuryTypeFilter = TreasuryTypeFilter.All
        set(value) {
            field = value
            rebuildItems()
        }

    var sortDescending: Boolean = true
        set(value) {
            field = value
            rebuildItems()
        }

    private fun rebuildItems() {
        fetch(forceRefresh = false)
    }

    private fun forceRefresh() {
        fetch(forceRefresh = true)
    }

    private fun fetch(forceRefresh: Boolean) {
        coroutineScope.launch {
            try {
                val coinTreasuries = repository.coinTreasuriesSingle(coin.uid, currency.code, treasuryType, sortDescending, forceRefresh).await()
                stateSubject.onNext(DataState.Success(coinTreasuries))
            } catch (e: Throwable) {
                stateSubject.onNext(DataState.Error(e))
            }
        }
    }

    fun start() {
        forceRefresh()
    }

    fun refresh() {
        forceRefresh()
    }

    fun stop() {
        coroutineScope.cancel()
    }
}
