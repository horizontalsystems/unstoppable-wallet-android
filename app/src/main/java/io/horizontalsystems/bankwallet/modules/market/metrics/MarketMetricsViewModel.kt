package io.horizontalsystems.bankwallet.modules.market.metrics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MarketMetricsViewModel(
        private val service: MarketMetricsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val marketMetricsLiveData = MutableLiveData<MarketMetricsWrapper?>(null)
    val toastLiveData = SingleLiveEvent<String>()
    val showGlobalMarketMetricsPage = SingleLiveEvent<MetricsType>()

    private var metricsWrapper: MarketMetricsWrapper? = null
        set(value) {
            field = value
            marketMetricsLiveData.postValue(value)
        }

    private val disposables = CompositeDisposable()

    init {
        service.marketMetricsObservable
                .subscribeIO {
                    syncMarketMetrics(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun refresh() {
        service.refresh()
    }

    private fun syncMarketMetrics(dataState: DataState<MarketMetricsItem>) {
        var loading = false
        val metricsNotSet = metricsWrapper?.marketMetrics == null
        if (metricsNotSet) {
            loading = dataState.loading
        }

        var showSyncError = false
        if (dataState is DataState.Error) {
            if (metricsNotSet) {
                showSyncError = true
            } else {
                toastLiveData.postValue(convertErrorMessage(dataState.error))
            }
        }

        var metrics = metricsWrapper?.marketMetrics

        if (dataState is DataState.Success) {
            val marketMetricsItem = dataState.data

            val btcDominanceFormatted = App.numberFormatter.format(marketMetricsItem.btcDominance, 0, 2, suffix = "%")
            val marketMetrics = MarketMetrics(
                    totalMarketCap = MetricData(formatFiatShortened(marketMetricsItem.marketCap.value, marketMetricsItem.marketCap.currency.symbol), marketMetricsItem.marketCapDiff24h, null),
                    btcDominance = MetricData(btcDominanceFormatted, marketMetricsItem.btcDominanceDiff24h, getChartData(marketMetricsItem.btcDominancePoints)),
                    volume24h = MetricData(formatFiatShortened(marketMetricsItem.volume24h.value, marketMetricsItem.volume24h.currency.symbol), marketMetricsItem.volume24hDiff24h, getChartData(marketMetricsItem.volume24Points)),
                    defiCap = MetricData(formatFiatShortened(marketMetricsItem.defiMarketCap.value, marketMetricsItem.defiMarketCap.currency.symbol), marketMetricsItem.defiMarketCapDiff24h, getChartData(marketMetricsItem.defiMarketCapPoints)),
                    defiTvl = MetricData(formatFiatShortened(marketMetricsItem.defiTvl.value, marketMetricsItem.defiTvl.currency.symbol), marketMetricsItem.defiTvlDiff24h, getChartData(marketMetricsItem.defiTvlPoints)),
            )

            metrics = marketMetrics
        }

        metricsWrapper = MarketMetricsWrapper(metrics, loading, showSyncError)
    }

    private fun getChartData(marketMetricsPoints: List<MarketMetricsPoint>): ChartData {
        val startTimestamp = marketMetricsPoints.first().timestamp
        val endTimestamp = marketMetricsPoints.last().timestamp
        val points = marketMetricsPoints.map { ChartPoint(it.value.toFloat(), null, it.timestamp) }
        return ChartDataFactory.build(points, startTimestamp, endTimestamp, false)
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val (shortenValue, suffix) = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    fun onBtcDominanceClick() {
        showGlobalMarketMetricsPage.postValue(MetricsType.BtcDominance)
    }

    fun on24VolumeClick() {
        showGlobalMarketMetricsPage.postValue(MetricsType.Volume24h)
    }

    fun onDefiCapClick() {
        showGlobalMarketMetricsPage.postValue(MetricsType.DefiCap)
    }

    fun onTvlInDefiClick() {
        showGlobalMarketMetricsPage.postValue(MetricsType.TvlInDefi)
    }

}

data class MarketMetrics(
        val totalMarketCap: MetricData,
        val btcDominance: MetricData,
        val volume24h: MetricData,
        val defiCap: MetricData,
        val defiTvl: MetricData,
)
