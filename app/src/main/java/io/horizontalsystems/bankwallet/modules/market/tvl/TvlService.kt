package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class TvlService(
    private val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) {
    private var currencyManagerDisposable: Disposable? = null
    private var globalMarketPointsDisposable: Disposable? = null
    private var tvlDataDisposable: Disposable? = null

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    val chartItemsObservable: BehaviorSubject<DataState<List<MetricChartModule.Item>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    val coinTvlItemsObservable: BehaviorSubject<DataState<List<TvlModule.CoinTvlItem>>> =
        BehaviorSubject.createDefault(DataState.Loading)

    var chartType: ChartView.ChartType = ChartView.ChartType.DAILY
        set(value) {
            field = value
            sync()
        }

    val chains: List<TvlModule.Chain> = TvlModule.Chain.values().toList()
    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            sync()
        }

    var sortDescending: Boolean = true
        set(value) {
            field = value
            sync()
        }

    private fun sync() {
        globalMarketPointsDisposable?.dispose()
        globalMarketRepository.getGlobalMarketPoints(baseCurrency.code, chartType)
            .doOnSubscribe { chartItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                chartItemsObservable.onNext(DataState.Success(it))
            }, {
                chartItemsObservable.onNext(DataState.Error(it))
            })
            .let { globalMarketPointsDisposable = it }


        tvlDataDisposable?.dispose()
        globalMarketRepository.getTvlData(baseCurrency, chain, chartType, sortDescending)
            .doOnSubscribe { coinTvlItemsObservable.onNext(DataState.Loading) }
            .subscribeIO({
                coinTvlItemsObservable.onNext(DataState.Success(it))
            }, {
                coinTvlItemsObservable.onNext(DataState.Error(it))
            })
            .let { tvlDataDisposable = it }
    }

    fun start() {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                sync()
            }
            .let { currencyManagerDisposable = it }
        sync()
    }


    fun refresh() {
        sync()
    }

    fun stop() {
        currencyManagerDisposable?.dispose()
        globalMarketPointsDisposable?.dispose()
        tvlDataDisposable?.dispose()
    }

}
