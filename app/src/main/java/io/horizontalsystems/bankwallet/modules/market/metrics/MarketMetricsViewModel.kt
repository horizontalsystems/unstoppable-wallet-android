package io.horizontalsystems.bankwallet.modules.market.metrics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MarketMetricsViewModel(
        private val service: MarketMetricsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val marketMetricsLiveData = MutableLiveData<MarketMetricsWrapper?>(null)
    val toastLiveData = MutableLiveData<String>()

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

        var error: String? = null
        if (dataState is DataState.Error) {
            if (metricsNotSet) {
                error = convertErrorMessage(dataState.error)
            } else {
                toastLiveData.postValue(convertErrorMessage(dataState.error))
            }
        }

        var metrics = metricsWrapper?.marketMetrics

        if (dataState is DataState.Success) {
            val marketMetricsItem = dataState.data

            val btcDominanceFormatted = App.numberFormatter.format(marketMetricsItem.btcDominance, 0, 2, suffix = "%")
            val marketMetrics = MarketMetrics(
                    totalMarketCap = MetricData(formatFiatShortened(marketMetricsItem.marketCap.value, marketMetricsItem.marketCap.currency.symbol), marketMetricsItem.marketCapDiff24h),
                    btcDominance = MetricData(btcDominanceFormatted, marketMetricsItem.btcDominanceDiff24h),
                    volume24h = MetricData(formatFiatShortened(marketMetricsItem.volume24h.value, marketMetricsItem.volume24h.currency.symbol), marketMetricsItem.volume24hDiff24h),
                    defiCap = MetricData(formatFiatShortened(marketMetricsItem.defiMarketCap.value, marketMetricsItem.defiMarketCap.currency.symbol), marketMetricsItem.defiMarketCapDiff24h),
                    defiTvl = MetricData(formatFiatShortened(marketMetricsItem.defiTvl.value, marketMetricsItem.defiTvl.currency.symbol), marketMetricsItem.defiTvlDiff24h),
            )

            metrics = marketMetrics
        }

        metricsWrapper = MarketMetricsWrapper(metrics, loading, error)
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
