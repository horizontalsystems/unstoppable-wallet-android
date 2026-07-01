package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class TvlService(
    private val currencyManager: CurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var tvlDataJob: Job? = null

    val currency by currencyManager::baseCurrency

    val marketTvlItemsObservable: BehaviorSubject<DataState<List<TvlModule.MarketTvlItem>>> =
        BehaviorSubject.create()

    private var chartInterval: HsTimePeriod? = HsTimePeriod.Day1
        set(value) {
            field = value
            updateTvlData(false)
        }

    val chains: List<TvlModule.Chain> = TvlModule.Chain.values().toList()
    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            updateTvlData(false)
        }

    var sortDescending: Boolean = true
        set(value) {
            field = value
            updateTvlData(false)
        }


    private fun forceRefresh() {
        updateTvlData(true)
    }

    private fun updateTvlData(forceRefresh: Boolean) {
        tvlDataJob?.cancel()
        tvlDataJob = coroutineScope.launch {
            try {
                val items = globalMarketRepository.getMarketTvlItems(
                    currency,
                    chain,
                    chartInterval,
                    sortDescending,
                    forceRefresh
                ).await()
                marketTvlItemsObservable.onNext(DataState.Success(items))
            } catch (e: Throwable) {
                marketTvlItemsObservable.onNext(DataState.Error(e))
            }
        }
    }

    fun start() {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                forceRefresh()
            }
        }

        forceRefresh()
    }


    fun refresh() {
        forceRefresh()
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun updateChartInterval(chartInterval: HsTimePeriod?) {
        this.chartInterval = chartInterval
    }
}
