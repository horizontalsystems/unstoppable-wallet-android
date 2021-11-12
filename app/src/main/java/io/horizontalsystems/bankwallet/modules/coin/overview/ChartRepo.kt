package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coin.LastPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartInfo
import io.horizontalsystems.marketkit.models.ChartType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class ChartRepo(
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val chartTypeStorage: IChartTypeStorage,
    private val coinUid: String
) {
    private val chartDataSubject = BehaviorSubject.create<DataState<Triple<ChartInfo, LastPoint, ChartType>>>()
    val chartDataObservable: Observable<DataState<Triple<ChartInfo, LastPoint, ChartType>>>
        get() = chartDataSubject

    private var chartType by chartTypeStorage::chartType

    private var chartInfo: ChartInfo? = null
    private var lastPoint: LastPoint? = null

    private val disposables = CompositeDisposable()

    fun start() {
        val currencyCode = currencyManager.baseCurrency.code

        chartInfo = marketKit.chartInfo(coinUid, currencyCode, chartType)
        lastPoint = marketKit.coinPrice(coinUid, currencyCode)?.let { coinPrice ->
            LastPoint(coinPrice.value, coinPrice.timestamp, coinPrice.diff)
        }
        if (chartInfo == null || lastPoint == null) {
            // show chart spinner only when chart data is not locally cached
            // and we need to wait for network response for data
            emitLoading()
        } else {
            emitData()
        }

        marketKit.getChartInfoAsync(coinUid, currencyCode, chartType)
            .subscribeIO({ chartInfo ->
                this.chartInfo = chartInfo
                emitData()
            }, {
                emitError(it)
            }).let {
                disposables.add(it)
            }

        marketKit.coinPriceObservable(coinUid, currencyCode)
            .subscribeIO({ coinPrice ->
                lastPoint = LastPoint(coinPrice.value, coinPrice.timestamp, coinPrice.diff)
                emitData()
            }, {
                emitError(it)
            })
            .let {
                disposables.add(it)
            }
    }

    fun stop() {
        disposables.clear()
    }

    fun changeChartType(chartType: ChartType) {
        this.chartType = chartType
        restart()
    }

    @Synchronized
    private fun emitLoading() {
        chartDataSubject.onNext(DataState.Loading)
    }

    @Synchronized
    private fun emitData() {
        val tmpChartInfo = chartInfo
        val tmpLastPoint = lastPoint
        if (tmpChartInfo != null && tmpLastPoint != null) {
            chartDataSubject.onNext(DataState.Success(Triple(tmpChartInfo, tmpLastPoint, chartType)))
        }
    }
    @Synchronized
    private fun emitError(error: Throwable) {
        chartDataSubject.onNext(DataState.Error(error))
    }

    private fun restart() {
        stop()
        start()
    }

}