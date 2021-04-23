package io.horizontalsystems.bankwallet.modules.market.marketglobal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.xrateskit.entities.GlobalCoinMarketPoint
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.util.*

class MarketGlobalViewModel(
        private val metricType: MetricsType,
        private val service: MarketGlobalService,
        private val numberFormatter: IAppNumberFormatter,
        private val clearables: List<MarketGlobalService>) : ViewModel() {

    val chartViewItemLiveData = MutableLiveData<ChartViewItem>()
    val selectedPointLiveData = MutableLiveData<SelectedPoint>()

    var chartType: ChartType = ChartType.DAILY

    val title: Int
        get() {
            return when (metricType) {
                MetricsType.BtcDominance -> R.string.MarketGlobalMetrics_BtcDominance
                MetricsType.Volume24h -> R.string.MarketGlobalMetrics_Volume
                MetricsType.DefiCap -> R.string.MarketGlobalMetrics_DefiCap
                MetricsType.TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefi
            }
        }

    val description: Int
        get() {
            return when (metricType) {
                MetricsType.BtcDominance -> R.string.MarketGlobalMetrics_BtcDominanceDescription
                MetricsType.Volume24h -> R.string.MarketGlobalMetrics_VolumeDescription
                MetricsType.DefiCap -> R.string.MarketGlobalMetrics_DefiCapDescription
                MetricsType.TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefiDescription
            }
        }

    private var chartData: ChartData? = null
    private var loading: Boolean = false
    private var topValueWithDiff: LastValueWithDiff? = null

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

    fun onChartTypeSelect(chartType: ChartType) {
        this.chartType = chartType
        fetchChartInfo()
    }

    fun onTouchSelect(point: PointInfo) {
        val value: String = getFormattedValue(point.value)
        val date = DateHelper.getDayAndTime(Date(point.timestamp * 1000))

        selectedPointLiveData.postValue(SelectedPoint(value, date))
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    private fun updateChartInfo() {
        val points = service.chartPoints ?: return

        chartData = getChartData(points)

        chartData?.let {
            topValueWithDiff = LastValueWithDiff(getTopValue(points.last()), it.diff())
        }

        loading = false

        updateChartViewItem()
    }

    private fun fetchChartInfo() {
        loading = true
        updateChartViewItem()
        service.updateChartInfo(chartType)
    }

    private fun updateChartViewItem() {
        val maxValue = chartData?.valueRange?.upper?.let { getFormattedValue(it) }
        val minValue = chartData?.valueRange?.lower?.let { getFormattedValue(it) }

        val chartViewItem = ChartViewItem(
                topValueWithDiff,
                chartData,
                maxValue,
                minValue,
                chartType,
                loading
        )

        chartViewItemLiveData.postValue(chartViewItem)
    }

    private fun getChartData(points: List<GlobalCoinMarketPoint>): ChartData {
        val startTimestamp = points.first().timestamp
        val endTimestamp = points.last().timestamp
        val metricPoints = points.map { ChartPoint(getPoint(it).toFloat(), null, it.timestamp) }
        return ChartDataFactory.build(metricPoints, startTimestamp, endTimestamp, false)
    }

    private fun getFormattedValue(value: Float): String {
        return when(metricType){
            MetricsType.BtcDominance -> numberFormatter.format(value, 0, 2, suffix = "%")
            MetricsType.Volume24h,
            MetricsType.DefiCap,
            MetricsType.TvlInDefi -> formatFiatShortened(value.toBigDecimal(), service.currency.symbol)
        }
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
        return numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
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
