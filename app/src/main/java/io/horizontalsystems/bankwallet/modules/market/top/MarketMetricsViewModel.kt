package io.horizontalsystems.bankwallet.modules.market.top

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.reactivex.disposables.CompositeDisposable

class MarketMetricsViewModel(private val service: MarketMetricsService) : ViewModel() {
    val marketMetricsLiveData = MutableLiveData<MarketMetrics?>(null)
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

    private val disposables = CompositeDisposable()

    init {
        service.marketMetricsObservable
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

    private fun syncMarketMetrics(dataState: DataState<MarketMetrics>) {
        loadingLiveData.postValue(dataState.loading)
        errorLiveData.postValue(dataState.errorOrNull?.let { convertErrorMessage(it) })
        if (dataState is DataState.Success) {
            marketMetricsLiveData.postValue(dataState.data)
        }
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    override fun onCleared() {
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
