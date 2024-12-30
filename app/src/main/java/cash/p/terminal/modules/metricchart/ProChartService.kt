package cash.p.terminal.modules.metricchart

import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPage
import cash.p.terminal.core.stats.statPeriod
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.models.HsTimePeriod
import io.reactivex.Single

class ProChartService(
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
    private val chartType: ProChartModule.ChartType
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1
    override val hasVolumes = chartType == ProChartModule.ChartType.TxCount

    override val chartIntervals = when (chartType) {
        ProChartModule.ChartType.CexVolume,
        ProChartModule.ChartType.DexVolume,
        ProChartModule.ChartType.TxCount,
        ProChartModule.ChartType.AddressesCount,
        ProChartModule.ChartType.DexLiquidity -> listOf(
            HsTimePeriod.Week1,
            HsTimePeriod.Week2,
            HsTimePeriod.Month1,
            HsTimePeriod.Month3,
            HsTimePeriod.Month6,
            HsTimePeriod.Year1,
        )
        ProChartModule.ChartType.Tvl -> listOf(
            HsTimePeriod.Day1,
            HsTimePeriod.Week1,
            HsTimePeriod.Week2,
            HsTimePeriod.Month1,
            HsTimePeriod.Month3,
            HsTimePeriod.Month6,
            HsTimePeriod.Year1,
        )
    }

    override val chartViewType = when (chartType) {
        ProChartModule.ChartType.Tvl,
        ProChartModule.ChartType.AddressesCount,
        ProChartModule.ChartType.DexLiquidity -> ChartViewType.Line
        ProChartModule.ChartType.CexVolume,
        ProChartModule.ChartType.DexVolume,
        ProChartModule.ChartType.TxCount -> ChartViewType.Bar
    }

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)

        stat(chartType.statPage, event = StatEvent.SwitchChartPeriod(chartInterval.statPeriod))
    }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        val chartDataSingle: Single<List<ChartPoint>> = when (chartType) {
            ProChartModule.ChartType.CexVolume ->
                marketKit.cexVolumesSingle(coinUid, currency.code, chartInterval)
                    .map { response ->
                        response.map { chartPoint ->
                            ChartPoint(
                                value = chartPoint.value.toFloat(),
                                timestamp = chartPoint.timestamp,
                                volume = chartPoint.volume?.toFloat()
                            )
                        }
                    }


            ProChartModule.ChartType.DexVolume ->
                marketKit.dexVolumesSingle(coinUid, currency.code, chartInterval)
                    .map { response ->
                        response.map { chartPoint ->
                            ChartPoint(
                                value = chartPoint.volume.toFloat(),
                                timestamp = chartPoint.timestamp,
                            )
                        }
                    }

            ProChartModule.ChartType.DexLiquidity ->
                marketKit.dexLiquiditySingle(coinUid, currency.code, chartInterval)
                    .map { response ->
                        response.map { chartPoint ->
                            ChartPoint(
                                value = chartPoint.volume.toFloat(),
                                timestamp = chartPoint.timestamp,
                            )
                        }
                    }

            ProChartModule.ChartType.TxCount ->
                marketKit.transactionDataSingle(coinUid, chartInterval, null)
                    .map { response ->
                        response.map { chartPoint ->
                            ChartPoint(
                                value = chartPoint.count.toFloat(),
                                timestamp = chartPoint.timestamp,
                                volume = chartPoint.volume.toFloat(),
                            )
                        }
                    }

            ProChartModule.ChartType.AddressesCount ->
                marketKit.activeAddressesSingle(coinUid, chartInterval)
                    .map { response ->
                        response.map { chartPoint ->
                            ChartPoint(
                                value = chartPoint.count.toFloat(),
                                timestamp = chartPoint.timestamp,
                            )
                        }
                    }

            ProChartModule.ChartType.Tvl ->
                marketKit.marketInfoTvlSingle(coinUid, currency.code, chartInterval)
                    .map { response ->
                        response.map { chartPoint ->
                            ChartPoint(
                                value = chartPoint.value.toFloat(),
                                timestamp = chartPoint.timestamp,
                                volume = chartPoint.volume?.toFloat(),
                            )
                        }
                    }
        }

        val isMovementChart = when (chartType) {
            ProChartModule.ChartType.DexLiquidity,
            ProChartModule.ChartType.AddressesCount,
            ProChartModule.ChartType.Tvl -> true
            ProChartModule.ChartType.CexVolume,
            ProChartModule.ChartType.DexVolume,
            ProChartModule.ChartType.TxCount -> false
        }

        return chartDataSingle.map { ChartPointsWrapper(it, isMovementChart) }
    }
}
