package io.horizontalsystems.bankwallet.modules.market.platform

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.bankwallet.modules.market.topplatforms.Platform
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class PlatformChartService(
    private val platform: Platform,
    override val currencyManager: ICurrencyManager,
    private val marketKit: MarketKitWrapper,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1
    override val chartIntervals = listOf(HsTimePeriod.Day1, HsTimePeriod.Week1, HsTimePeriod.Month1)

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> = try {
        marketKit.topPlatformMarketCapPointsSingle(platform.uid, chartInterval, currency.code)
            .map { info -> info.map { ChartPoint(it.marketCap.toFloat(), it.timestamp) } }
            .map { ChartPointsWrapper(chartInterval, it) }
    } catch (e: Exception) {
        Single.error(e)
    }

}
