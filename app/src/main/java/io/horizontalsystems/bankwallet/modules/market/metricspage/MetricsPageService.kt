package io.horizontalsystems.bankwallet.modules.market.metricspage

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.tvl.GlobalMarketRepository
import io.horizontalsystems.bankwallet.modules.market.tvl.XxxChartServiceRepo
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject


class MetricsPageChartServiceRepo(
    val metricsType: MetricsType,
    private val globalMarketRepository: GlobalMarketRepository
) : XxxChartServiceRepo {
    override val chartTypes = listOf(ChartView.ChartType.DAILY, ChartView.ChartType.WEEKLY, ChartView.ChartType.MONTHLY)
    override val dataUpdatedObservable = BehaviorSubject.create<Unit>()

    override fun getItems(
        chartType: ChartView.ChartType,
        currency: Currency,
    ): Single<List<MetricChartModule.Item>> {
        return globalMarketRepository.getGlobalMarketPoints(currency.code, chartType, metricsType)
    }
}

class MetricsPageService(
    val metricsType: MetricsType,
    private val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var marketDataDisposable: Disposable? = null

    val currency by currencyManager::baseCurrency

    val marketItemsObservable: BehaviorSubject<DataState<List<MarketItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    var sortDescending: Boolean = true
        set(value) {
            field = value
            syncMarketItems()
        }

    private fun sync() {
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
