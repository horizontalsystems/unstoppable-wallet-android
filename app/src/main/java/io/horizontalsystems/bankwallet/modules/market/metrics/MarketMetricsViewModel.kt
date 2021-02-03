package io.horizontalsystems.bankwallet.modules.market.metrics

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.horizontalsystems.xrateskit.entities.GlobalCoinMarket
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncMarketMetrics(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun refresh() {
        service.refresh()
    }

    private fun syncMarketMetrics(dataState: DataState<GlobalCoinMarket>) {
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
            val globalMarketInfo = dataState.data

            val symbol = service.currency.symbol
            val btcDominanceFormatted = App.numberFormatter.format(globalMarketInfo.btcDominance, 0, 2, suffix = "%")
            val marketMetrics = MarketMetrics(
                    totalMarketCap = MetricData(formatFiatShortened(globalMarketInfo.marketCap, symbol), globalMarketInfo.marketCapDiff24h),
                    btcDominance = MetricData(btcDominanceFormatted, globalMarketInfo.btcDominanceDiff24h),
                    volume24h = MetricData(formatFiatShortened(globalMarketInfo.volume24h, symbol), globalMarketInfo.volume24hDiff24h),
                    defiCap = MetricData(formatFiatShortened(globalMarketInfo.defiMarketCap, symbol), globalMarketInfo.defiMarketCapDiff24h),
                    defiTvl = MetricData(formatFiatShortened(globalMarketInfo.defiTvl, symbol), globalMarketInfo.defiTvlDiff24h),
            )

            metrics = marketMetrics
        }

        metricsWrapper = MarketMetricsWrapper(metrics, loading, error)
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val (shortenValue, suffix) = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + suffix
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
