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

    val toastLiveData = SingleLiveEvent<String>()
    val stateLiveData = MutableLiveData<MarketMetricsModule.State>()

    private var marketMetrics: MarketMetrics? = null

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
        val metricsNotSet = marketMetrics == null
        if (metricsNotSet) {
            stateLiveData.postValue(MarketMetricsModule.State.Loading)
        }

        if (dataState is DataState.Error) {
            if (metricsNotSet) {
                stateLiveData.postValue(MarketMetricsModule.State.SyncError)
            } else {
                toastLiveData.postValue(convertErrorMessage(dataState.error))
            }
        }

        if (dataState is DataState.Success) {
            val marketMetricsItem = dataState.data

            val btcDominanceFormatted = App.numberFormatter.format(marketMetricsItem.btcDominance, 0, 2, suffix = "%")
            val metrics = MarketMetrics(
                    totalMarketCap = MetricData(
                        formatFiatShortened(marketMetricsItem.marketCap.value, marketMetricsItem.marketCap.currency.symbol),
                        marketMetricsItem.marketCapDiff24h,
                        getChartData(marketMetricsItem.totalMarketCapPoints),
                        MetricsType.TotalMarketCap
                    ),
                    btcDominance = MetricData(
                        btcDominanceFormatted,
                        marketMetricsItem.btcDominanceDiff24h,
                        getChartData(marketMetricsItem.btcDominancePoints),
                        MetricsType.BtcDominance
                    ),
                    volume24h = MetricData(
                        formatFiatShortened(marketMetricsItem.volume24h.value, marketMetricsItem.volume24h.currency.symbol),
                        marketMetricsItem.volume24hDiff24h,
                        getChartData(marketMetricsItem.volume24Points),
                        MetricsType.Volume24h
                    ),
                    defiCap = MetricData(
                        formatFiatShortened(marketMetricsItem.defiMarketCap.value, marketMetricsItem.defiMarketCap.currency.symbol),
                        marketMetricsItem.defiMarketCapDiff24h,
                        getChartData(marketMetricsItem.defiMarketCapPoints),
                        MetricsType.DefiCap
                    ),
                    defiTvl = MetricData(
                        formatFiatShortened(marketMetricsItem.defiTvl.value, marketMetricsItem.defiTvl.currency.symbol),
                        marketMetricsItem.defiTvlDiff24h,
                        getChartData(marketMetricsItem.defiTvlPoints),
                        MetricsType.TvlInDefi
                    ),
            )

            marketMetrics = metrics
            stateLiveData.postValue(MarketMetricsModule.State.Data(metrics))
        }
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

}

data class MarketMetrics(
        val totalMarketCap: MetricData,
        val btcDominance: MetricData,
        val volume24h: MetricData,
        val defiCap: MetricData,
        val defiTvl: MetricData,
)
