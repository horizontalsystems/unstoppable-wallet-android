package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.market.tvl.XxxChartService
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MetricsPageService(
    val metricsType: MetricsType,
    private val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) : XxxChartService {
    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var marketDataDisposable: Disposable? = null

    override val currency by currencyManager::baseCurrency
    override val chartItemsObservable = BehaviorSubject.createDefault<DataState<Pair<ChartView.ChartType, List<MetricChartModule.Item>>>>(DataState.Loading)
    override val chartTypeObservable = BehaviorSubject.create<ChartView.ChartType>()
    override val chartTypes = listOf(ChartView.ChartType.DAILY, ChartView.ChartType.WEEKLY, ChartView.ChartType.MONTHLY)

    override fun updateChartType(chartType: ChartView.ChartType) {
        this.chartType = chartType
    }

    val marketItemsObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private var chartType: ChartView.ChartType = ChartView.ChartType.DAILY
        set(value) {
            field = value
            chartTypeObservable.onNext(value)
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
        globalMarketRepository.getMarketItems(currency, sortDescending, metricsType)
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
        globalMarketRepository.getGlobalMarketPoints(currency.code, chartType, metricsType)
            .doOnSubscribe { chartItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                chartItemsObservable.onNext(DataState.Success(Pair(chartType, it)))
            }, {
                chartItemsObservable.onNext(DataState.Error(it))
            })
            .let { globalMarketPointsDisposable = it }
    }

    fun start() {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO { sync() }
            .let { currencyManagerDisposable = it }

        chartTypeObservable.onNext(chartType)
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
