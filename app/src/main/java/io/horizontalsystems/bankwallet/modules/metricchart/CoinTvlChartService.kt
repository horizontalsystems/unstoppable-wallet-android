package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class CoinTvlChartService(
    override val currencyManager: ICurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1
    override val chartIntervals = HsTimePeriod.values().toList()

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> = try {
        marketKit.marketInfoTvlSingle(coinUid, currency.code, chartInterval)
            .map { info ->
                info.map { ChartPoint(it.value.toFloat(), it.timestamp) }
            }
            .map { ChartPointsWrapper(chartInterval, it) }
    } catch (e: Exception) {
        Single.error(e)
    }

}
