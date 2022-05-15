package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.ChartPointType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class CoinTradingVolumeChartService(
    override val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1

    override val chartIntervals = HsTimePeriod.values().toList()

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        return marketKit.chartInfoSingle(coinUid, currency.code, chartInterval)
            .map { info ->
                val items = info.points
                    .filter { it.timestamp >= info.startTimestamp }
                    .mapNotNull { chartPoint ->
                        chartPoint.extra[ChartPointType.Volume]?.let {
                            ChartPoint(it.toFloat(), chartPoint.timestamp)
                        }
                    }

                ChartPointsWrapper(
                    chartInterval,
                    items,
                    info.startTimestamp,
                    info.endTimestamp,
                    info.isExpired
                )
            }
    }
}
