package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.chart.IChartRepo
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.kitChartType
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartInfo
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinOverviewChartRepo(
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val chartTypeStorage: IChartTypeStorage,
    private val coinUid: String,
) : IChartRepo {

    override val chartTypes: List<ChartView.ChartType>
        get() = listOf(
            ChartView.ChartType.TODAY,
            ChartView.ChartType.DAILY,
            ChartView.ChartType.WEEKLY,
            ChartView.ChartType.WEEKLY2,
            ChartView.ChartType.MONTHLY,
            ChartView.ChartType.MONTHLY3,
            ChartView.ChartType.MONTHLY6,
            ChartView.ChartType.MONTHLY12,
            ChartView.ChartType.MONTHLY24
        )

    override val initialChartType by chartTypeStorage::chartType2
    override val dataUpdatedObservable = BehaviorSubject.create<Unit>()

    private var chartInfo: ChartInfo? = null
    private var lastCoinPrice: CoinPrice? = null

    private val disposables = CompositeDisposable()

    override fun start() {

    }

    override fun stop() {
        unsubscribeFromUpdates()
    }

    override fun getItems(
        chartType: ChartView.ChartType,
        currency: Currency,
    ): Single<List<MetricChartModule.Item>> {
        unsubscribeFromUpdates()
        subscribeForUpdates(currency, chartType)


        marketKit.chartInfo(coinUid, currency.code, chartType.kitChartType)?.let {
            Single.just(it)
        } ?: marketKit.chartInfoSingle(coinUid, currency.code, chartType.kitChartType)

        val tmpChartInfo = marketKit.chartInfo(coinUid, currency.code, chartType.kitChartType)
        val tmpLastCoinPrice = marketKit.coinPrice(coinUid, currency.code)

        val items = if (tmpChartInfo != null && tmpLastCoinPrice != null) {
            doGetItems(tmpChartInfo, tmpLastCoinPrice, chartType)
        } else {
            listOf()
        }

        return Single.just(items)
    }

    private fun unsubscribeFromUpdates() {
        disposables.clear()
    }

    private fun subscribeForUpdates(currency: Currency, chartType: ChartView.ChartType) {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                dataUpdatedObservable.onNext(Unit)
            }
            .let {
                disposables.add(it)
            }

        marketKit.getChartInfoAsync(coinUid, currency.code, chartType.kitChartType)
            .subscribeIO {
                dataUpdatedObservable.onNext(Unit)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun doGetItems(
        chartInfo: ChartInfo,
        lastCoinPrice: CoinPrice,
        chartType: ChartView.ChartType
    ): List<MetricChartModule.Item> {
        val points = chartInfo.points
        if (points.isEmpty()) return listOf()

        val items = points
            .map {
                MetricChartModule.Item(it.value, null, it.timestamp)
            }
            .toMutableList()

        if (lastCoinPrice.timestamp > items.last().timestamp) {
            items.add(MetricChartModule.Item(lastCoinPrice.value, null, lastCoinPrice.timestamp))

            if (chartType == ChartView.ChartType.DAILY) {
                val startTimestamp = lastCoinPrice.timestamp - 24 * 60 * 60
                val startValue = (lastCoinPrice.value * 100.toBigDecimal()) / (lastCoinPrice.diff + 100.toBigDecimal())
                val startItem = MetricChartModule.Item(startValue, null, startTimestamp)

                items.removeIf { it.timestamp <= startTimestamp }
                items.add(0, startItem)
            }
        }

        items.removeIf { it.timestamp < chartInfo.startTimestamp }

        return items
    }

//    @Synchronized
//    private fun emitLoading() {
//        chartDataSubject.onNext(DataState.Loading)
//    }
//
//    @Synchronized
//    private fun emitData() {
//        val tmpChartInfo = chartInfo
//        val tmpLastPoint = lastPoint
//        if (tmpChartInfo != null && tmpLastPoint != null) {
//            chartDataSubject.onNext(DataState.Success(Triple(tmpChartInfo, tmpLastPoint, chartType)))
//        }
//    }
//    @Synchronized
//    private fun emitError(error: Throwable) {
//        chartDataSubject.onNext(DataState.Error(error))
//    }
//

}
