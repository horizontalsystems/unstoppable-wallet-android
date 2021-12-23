package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartDataXxx
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class TvlChartRepo(
    override val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
): AbstractChartService() {

    override val chartTypes = listOf(ChartView.ChartType.DAILY, ChartView.ChartType.WEEKLY, ChartView.ChartType.MONTHLY)
    override val initialChartType: ChartView.ChartType = ChartView.ChartType.DAILY

    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            dataInvalidated()
        }

    override fun getItems(chartType: ChartView.ChartType, currency: Currency): Single<ChartDataXxx> {
        val chainParam = if (chain == TvlModule.Chain.All) "" else chain.name
        return globalMarketRepository.getTvlGlobalMarketPoints(chainParam, currency.code, chartType)
            .map {
                ChartDataXxx(chartType, it)
            }
    }
}

class TvlService(
    private val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository,
    private val chartRepo: TvlChartRepo
) {

    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var tvlDataDisposable: Disposable? = null

    val currency by currencyManager::baseCurrency

    val marketTvlItemsObservable: BehaviorSubject<DataState<List<TvlModule.MarketTvlItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    private var chartType: ChartView.ChartType = ChartView.ChartType.DAILY
        set(value) {
            field = value
            updateTvlData(false)
        }

    val chains: List<TvlModule.Chain> = TvlModule.Chain.values().toList()
    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            updateTvlData(false)
            chartRepo.chain = chain
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
