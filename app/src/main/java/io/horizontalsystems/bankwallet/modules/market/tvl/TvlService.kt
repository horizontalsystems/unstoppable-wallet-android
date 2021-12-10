package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class TvlService(
    private val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    val chartTypes = listOf(ChartView.ChartType.DAILY, ChartView.ChartType.WEEKLY, ChartView.ChartType.MONTHLY)

    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var tvlDataDisposable: Disposable? = null

    val currency: Currency
        get() = currencyManager.baseCurrency

    val chartItemsObservable: BehaviorSubject<DataState<Pair<ChartView.ChartType, List<MetricChartModule.Item>>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val marketTvlItemsObservable: BehaviorSubject<DataState<List<TvlModule.MarketTvlItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private var chartType: ChartView.ChartType = ChartView.ChartType.DAILY
        set(value) {
            field = value
            chartTypeSubject.onNext(value)
            updateGlobalMarketPoints()
            updateTvlData(false)
        }

    private val chartTypeSubject = BehaviorSubject.createDefault(chartType)
    val chartTypeObservable: Observable<ChartView.ChartType> = chartTypeSubject

    val chains: List<TvlModule.Chain> = TvlModule.Chain.values().toList()
    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            updateGlobalMarketPoints()
            updateTvlData(false)
        }

    var sortDescending: Boolean = true
        set(value) {
            field = value
            updateTvlData(false)
        }

    private fun updateGlobalMarketPoints() {
        globalMarketPointsDisposable?.dispose()

        val chainParam = if (chain == TvlModule.Chain.All) "" else chain.name
        globalMarketRepository.getTvlGlobalMarketPoints(chainParam, currency.code, chartType)
            .doOnSubscribe { chartItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                chartItemsObservable.onNext(DataState.Success(Pair(chartType, it)))
            }, {
                chartItemsObservable.onNext(DataState.Error(it))
            })
            .let { globalMarketPointsDisposable = it }
    }

    private fun forceRefresh() {
        updateGlobalMarketPoints()
        updateTvlData(true)
    }

    private fun updateTvlData(forceRefresh: Boolean) {
        tvlDataDisposable?.dispose()
        globalMarketRepository.getMarketTvlItems(currency, chain, chartType, sortDescending, forceRefresh)
            .doOnSubscribe { marketTvlItemsObservable.onNext(DataState.Loading) }
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

    fun updateChartType(chartType: ChartView.ChartType) {
        this.chartType = chartType
    }
}
