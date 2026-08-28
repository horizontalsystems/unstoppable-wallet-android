package io.horizontalsystems.walletkit.modules.market.earn.vault

import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.ChartVolume
import io.horizontalsystems.chartview.models.ChartVolumeType
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.stats.statPeriod
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.chart.AbstractChartService
import io.horizontalsystems.walletkit.modules.chart.ChartPointsWrapper
import io.horizontalsystems.marketkit.models.HsTimePeriod
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

    override suspend fun getAllItems(currency: Currency): ChartPointsWrapper {
        return getChartPointsWrapper(initialChartInterval)
    }

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): ChartPointsWrapper {
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

    private suspend fun getChartPointsWrapper(
        periodType: HsTimePeriod,
    ): ChartPointsWrapper {
        val vault = marketKit.vault(vaultAddress, currencyManager.baseCurrency.code, periodType)
        val points = vault.chart.map { point ->
            ChartPoint(
                value = point.apy.toFloat(),
                timestamp = point.timestamp.toLong(),
                chartVolume = ChartVolume(point.tvl.toFloat(), ChartVolumeType.Tvl),
            )
        }
        return ChartPointsWrapper(points)
    }
}
