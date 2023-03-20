package io.horizontalsystems.marketkit.chart

import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.marketkit.providers.HsProvider
import io.reactivex.Single

class ChartManager(private val provider: HsProvider) {

    fun chartInfoSingle(
        coinUid: String,
        currencyCode: String,
        periodType: HsPeriodType
    ): Single<List<ChartPoint>> {
        return provider.coinPriceChartSingle(
            coinUid,
            currencyCode,
            periodType
        ).map { response ->
            response.map { it.chartPoint }
        }
    }

    fun chartStartTimeSingle(coinUid: String): Single<Long> {
        return provider.coinPriceChartStartTime(coinUid)
    }
}
