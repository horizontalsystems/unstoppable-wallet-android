package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.extensions.MetricData
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import java.util.concurrent.Executors

class MarketMetricsService {

    val marketMetricsObservable: BehaviorSubject<DataState<MarketMetrics>> = BehaviorSubject.createDefault(DataState.Loading)

    private val bgThread = Executors.newCachedThreadPool()
    init {
        fetchMarketMetrics()
    }

    fun refresh() {
        fetchMarketMetrics()
    }



    private fun fetchMarketMetrics() {
        bgThread.execute {

            marketMetricsObservable.onNext(DataState.Loading)

            Thread.sleep(1000)

            val marketMetrics = MarketMetrics(
                    MetricData("$555.61B", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
                    MetricData("69.09%", stubPercentage()),
            )

            marketMetricsObservable.onNext(DataState.Success(marketMetrics))
        }

    }

    private fun stubPercentage(): BigDecimal {
        return (Math.random() - Math.random()).times(100).toBigDecimal()
    }


}
