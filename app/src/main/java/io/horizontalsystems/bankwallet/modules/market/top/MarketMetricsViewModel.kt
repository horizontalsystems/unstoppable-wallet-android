package io.horizontalsystems.bankwallet.modules.market.top

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import java.math.BigDecimal
import java.util.concurrent.Executors

class MarketMetricsViewModel : ViewModel() {

    private val bgThread = Executors.newCachedThreadPool()
    val metricsLiveData = MutableLiveData<MarketMetrics>()

    init {
        fetchMarketMetrics()
    }

    fun refresh() {
        fetchMarketMetrics()
    }


    private fun fetchMarketMetrics() {
        bgThread.submit {
            Thread.sleep(1000)

            metricsLiveData.postValue(MarketMetrics(
                    MetricData("$555.61B", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
            ))
        }
    }

    private fun stubPercentage(): BigDecimal {
        return (Math.random() - Math.random()).times(100).toBigDecimal()
    }
}

data class MarketMetrics(
        val totalMarketCap: MetricData,
        val btcDominance: MetricData,
        val volume24h: MetricData,
        val defiCap: MetricData,
        val defiTvl: MetricData,
)
