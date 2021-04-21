package io.horizontalsystems.bankwallet.modules.market.marketglobal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.xrateskit.entities.GlobalCoinMarketPoint
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MarketGlobalViewModel(
        private val metricType: MetricsType,
        private val service: MarketGlobalService,
        private val numberFormatter: IAppNumberFormatter,
        private val clearables: List<MarketGlobalService>) : ViewModel() {

    val chartViewItem = MutableLiveData<ChartViewItem>()

    var chartType: ChartType = ChartType.DAILY

    val title: Int
        get() {
            return when (metricType) {
                MetricsType.BtcDominance -> R.string.MarketGlobalMetrics_BitcoinDominance
                MetricsType.Volume24h -> R.string.MarketGlobalMetrics_Volume
                MetricsType.DefiCap -> R.string.MarketGlobalMetrics_DefiCap
                MetricsType.TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefi
            }
        }

    private var chartData: ChartData? = null
    private var loading: Boolean = false
    private var topValueWithDiff: TopValueWithDiff? = null

    private val disposables = CompositeDisposable()

    init {
        fetchChartInfo()

        service.chartPointsUpdatedObservable
                .subscribeIO {
                    updateChartInfo()
                }
                .let {
                    disposables.add(it)
                }

    }

    fun onSelect(chartType: ChartType) {
        this.chartType = chartType
        fetchChartInfo()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    private fun updateChartInfo() {
        val points = service.chartPoints?.reversed() ?: return

        val chartData = getChartData(points)

        topValueWithDiff = TopValueWithDiff(getTopValue(points.last()), chartData.diff())

        this.chartData = chartData

        loading = false

        updateChartViewItem()
    }

    private fun fetchChartInfo() {
        loading = true
        updateChartViewItem()
        service.updateChartInfo(chartType)
    }

    private fun updateChartViewItem() {
        chartViewItem.postValue(ChartViewItem(topValueWithDiff, chartData, chartType, service.currency, loading))
    }

    private fun getChartData(points: List<GlobalCoinMarketPoint>): ChartData {
        val startTimestamp = points.first().timestamp
        val endTimestamp = points.last().timestamp
        val metricPoints = points.map { ChartPoint(getPoint(it).toFloat(), null, it.timestamp) }
        return ChartDataFactory.build(metricPoints, startTimestamp, endTimestamp, false)
    }

    private fun getPoint(point: GlobalCoinMarketPoint): BigDecimal {
        return when (metricType) {
            MetricsType.BtcDominance -> point.btcDominance
            MetricsType.Volume24h -> point.volume24h
            MetricsType.DefiCap -> point.defiMarketCap
            MetricsType.TvlInDefi -> point.defiTvl
        }
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val (shortenValue, suffix) = numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
    }

    private fun getTopValue(point: GlobalCoinMarketPoint): String {
        return when (metricType) {
            MetricsType.BtcDominance -> numberFormatter.format(point.btcDominance, 0, 2, suffix = "%")
            MetricsType.Volume24h -> formatFiatShortened(point.volume24h, service.currency.symbol)
            MetricsType.DefiCap -> formatFiatShortened(point.defiMarketCap, service.currency.symbol)
            MetricsType.TvlInDefi -> formatFiatShortened(point.defiTvl, service.currency.symbol)
        }
    }

}
