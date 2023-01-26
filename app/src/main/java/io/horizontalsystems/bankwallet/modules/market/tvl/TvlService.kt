package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class TvlService(
    private val currencyManager: CurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {

    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var tvlDataDisposable: Disposable? = null

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
        tvlDataDisposable?.dispose()
        globalMarketRepository.getMarketTvlItems(currency, chain, chartInterval, sortDescending, forceRefresh)
            .subscribeIO({
                marketTvlItemsObservable.onNext(DataState.Success(it))
            }, {
                marketTvlItemsObservable.onNext(DataState.Error(it))
            })
            .let { tvlDataDisposable = it }
    }

    fun start() {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                forceRefresh()
            }
            .let { currencyManagerDisposable = it }

        forceRefresh()
    }


    fun refresh() {
        forceRefresh()
    }

    fun stop() {
        currencyManagerDisposable?.dispose()
        globalMarketPointsDisposable?.dispose()
        tvlDataDisposable?.dispose()
    }

    fun updateChartInterval(chartInterval: HsTimePeriod?) {
        this.chartInterval = chartInterval
    }
}
