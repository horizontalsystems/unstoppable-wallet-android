package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class MetricsPageService(
    val metricsType: MetricsType,
    private val currencyManager: CurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var marketDataJob: Job? = null

    val currency by currencyManager::baseCurrency

    val marketItemsObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.create()

    var sortDescending: Boolean = true
        set(value) {
            field = value
            syncMarketItems()
        }

    private fun syncMarketItems() {
        marketDataJob?.cancel()
        marketDataJob = coroutineScope.launch {
            try {
                val marketItems = globalMarketRepository
                    .getMarketItems(currency, sortDescending, metricsType)
                    .await()
                marketItemsObservable.onNext(DataState.Success(marketItems))
            } catch (e: Throwable) {
                marketItemsObservable.onNext(DataState.Error(e))
            }
        }
    }

    fun start() {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                syncMarketItems()
            }
        }

        syncMarketItems()
    }

    fun refresh() {
        syncMarketItems()
    }

    fun stop() {
        coroutineScope.cancel()
    }
}
