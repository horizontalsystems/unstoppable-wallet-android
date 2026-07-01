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
import io.horizontalsystems.chartview.models.ChartVolume
import io.horizontalsystems.chartview.models.ChartVolumeType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import java.math.BigDecimal

class VaultChartService(
    private val vaultAddress: String,
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) : AbstractChartService() {

    override val hasVolumes = true
    override val initialChartInterval = HsTimePeriod.Week1
    override val chartIntervals = listOf(
        HsTimePeriod.Day1,
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

    override fun chartPointsDiff(items: List<ChartPoint>): BigDecimal {
        val values = items.map { it.value }
        if (values.isEmpty()) {
            return BigDecimal.ZERO
        }

        val firstValue = values.find { it != 0f }
        val lastValue = values.last()
        if (lastValue == 0f || firstValue == null) {
            return BigDecimal.ZERO
        }

        return try {
            (lastValue - firstValue).toBigDecimal()
        } catch(e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun getChartPointsWrapper(
        periodType: HsTimePeriod,
    ): Single<ChartPointsWrapper> {
        return try {
            marketKit.vault(vaultAddress, currencyManager.baseCurrency.code, periodType)
                .map { vault ->
                    vault.chart.map { point ->
                        ChartPoint(
                            value = point.apy.toFloat(),
                            timestamp = point.timestamp.toLong(),
                            chartVolume = ChartVolume(point.tvl.toFloat(), ChartVolumeType.Tvl),
                        )
                    }
                }
                .map { ChartPointsWrapper(it) }
        } catch (e: Exception) {
            Single.error(e)
        }
    }
}
