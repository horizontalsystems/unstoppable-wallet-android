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
    val marketMetricsLiveData = MutableLiveData<MarketMetrics?>(null)
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

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
        loadingLiveData.postValue(dataState.loading)
        errorLiveData.postValue(dataState.errorOrNull?.let { convertErrorMessage(it) })
        if (dataState is DataState.Success) {
            val globalMarketInfo = dataState.data

            val symbol = service.currency.symbol
            val btcDominanceFormatted = App.numberFormatter.format(globalMarketInfo.btcDominance, 0, 2, suffix = "%")
            val marketMetrics = MarketMetrics(
                    totalMarketCap = MetricData(formatFiatShortened(globalMarketInfo.marketCap, symbol), globalMarketInfo.marketCapDiff24h),
                    btcDominance = MetricData(btcDominanceFormatted, globalMarketInfo.btcDominanceDiff24h),
                    volume24h = MetricData(formatFiatShortened(globalMarketInfo.volume24h, symbol), globalMarketInfo.volume24hDiff24h),
                    defiCap = MetricData(formatFiatShortened(globalMarketInfo.defiMarketCap, symbol), null),
                    defiTvl = MetricData(null, null),
            )

            marketMetricsLiveData.postValue(marketMetrics)
        }
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
