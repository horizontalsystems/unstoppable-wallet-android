package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MetricsPageService(
    val metricsType: MetricsType,
    private val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var marketDataDisposable: Disposable? = null

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    val chartItemsObservable: BehaviorSubject<DataState<List<MetricChartModule.Item>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val marketItemsObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    var chartType: ChartView.ChartType = ChartView.ChartType.DAILY
        set(value) {
            field = value
            syncChartItems()
        }

    var sortDescending: Boolean = true
        set(value) {
            field = value
            syncMarketItems()
        }

    private fun sync() {
        syncChartItems()

        syncMarketItems()
    }

    private fun syncMarketItems() {
        marketDataDisposable?.dispose()
        globalMarketRepository.getMarketItems(baseCurrency, sortDescending, metricsType)
            .doOnSubscribe { marketItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                marketItemsObservable.onNext(DataState.Success(it))
            }, {
                marketItemsObservable.onNext(DataState.Error(it))
            })
            .let { marketDataDisposable = it }
    }

    private fun syncChartItems() {
        globalMarketPointsDisposable?.dispose()
        globalMarketRepository.getGlobalMarketPoints(baseCurrency.code, chartType, metricsType)
            .doOnSubscribe { chartItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                chartItemsObservable.onNext(DataState.Success(it))
            }, {
                chartItemsObservable.onNext(DataState.Error(it))
            })
            .let { globalMarketPointsDisposable = it }
    }

    fun start() {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { sync() }
            .let { currencyManagerDisposable = it }
        sync()
    }

    fun refresh() {
        sync()
    }

    fun stop() {
        currencyManagerDisposable?.dispose()
        globalMarketPointsDisposable?.dispose()
        marketDataDisposable?.dispose()
    }
}
