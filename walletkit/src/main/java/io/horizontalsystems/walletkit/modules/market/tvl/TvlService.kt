package io.horizontalsystems.walletkit.modules.market.tvl

import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TvlService(
    private val currencyManager: CurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var tvlDataJob: Job? = null

    val currency by currencyManager::baseCurrency

    private val _marketTvlItemsFlow = MutableSharedFlow<DataState<List<TvlModule.MarketTvlItem>>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val marketTvlItemsFlow: Flow<DataState<List<TvlModule.MarketTvlItem>>> = _marketTvlItemsFlow.asSharedFlow()

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
                )
                _marketTvlItemsFlow.tryEmit(DataState.Success(items))
            } catch (e: Throwable) {
                _marketTvlItemsFlow.tryEmit(DataState.Error(e))
            }
        }
    }

    fun start() {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
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
