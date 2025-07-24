package io.horizontalsystems.bankwallet.modules.market.earn.vault

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPeriod
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class VaultChartService(
    private val vaultAddress: String,
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Week1
    override val chartIntervals = listOf(
        HsTimePeriod.Week1,
        HsTimePeriod.Week2,
        HsTimePeriod.Month1,
        HsTimePeriod.Month3,
    )
    override val chartViewType = ChartViewType.Line

    override fun getAllItems(currency: Currency): Single<ChartPointsWrapper> {
        return getChartPointsWrapper(initialChartInterval)
    }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        return getChartPointsWrapper(chartInterval)
    }

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)

        stat(
            page = StatPage.TopPlatform,
            event = StatEvent.SwitchChartPeriod(chartInterval.statPeriod)
        )
    }

    private fun getChartPointsWrapper(
        periodType: HsTimePeriod,
    ): Single<ChartPointsWrapper> {
        return try {
            marketKit.vault(vaultAddress, currencyManager.baseCurrency.code, periodType)
                .map { vault ->
                    vault.apyChart.map {
                        ChartPoint(it.apy.toFloat() * 100, it.timestamp.toLong())
                    }
                }
                .map { ChartPointsWrapper(it, customHint = "APY (" + periodType.value.uppercase() + ")") }
        } catch (e: Exception) {
            Single.error(e)
        }
    }
}
