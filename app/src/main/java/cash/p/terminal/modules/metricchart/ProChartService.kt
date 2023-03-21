package cash.p.terminal.modules.metricchart

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartPointsWrapper
import cash.p.terminal.modules.profeatures.ProFeaturesAuthorizationManager
import cash.p.terminal.modules.profeatures.ProNft
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.ChartPointType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class ProChartService(
    override val currencyManager: CurrencyManager,
    private val proFeaturesAuthorizationManager: ProFeaturesAuthorizationManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
    private val chartType: ProChartModule.ChartType
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1
    override val hasVolumes = chartType == ProChartModule.ChartType.TxCount

    override val chartIntervals = HsTimePeriod.values().toList()
    override val chartViewType = when (chartType) {
        ProChartModule.ChartType.Tvl,
        ProChartModule.ChartType.DexLiquidity -> ChartViewType.Line
        ProChartModule.ChartType.DexVolume,
        ProChartModule.ChartType.AddressesCount,
        ProChartModule.ChartType.TxCount -> ChartViewType.Bar
    }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): Single<ChartPointsWrapper> {
        val sessionKey = proFeaturesAuthorizationManager.getSessionKey(ProNft.YAK)?.key?.value

        val chartDataSingle: Single<List<io.horizontalsystems.marketkit.models.ChartPoint>> = when (chartType) {
            ProChartModule.ChartType.DexVolume ->
                marketKit.dexVolumesSingle(coinUid, currency.code, chartInterval, sessionKey)
                    .map { response -> response.volumePoints }

            ProChartModule.ChartType.DexLiquidity ->
                marketKit.dexLiquiditySingle(coinUid, currency.code, chartInterval, sessionKey)
                    .map { response -> response.volumePoints }

            ProChartModule.ChartType.TxCount ->
                marketKit.transactionDataSingle(coinUid, currency.code, chartInterval, null, sessionKey)
                    .map { response -> response.countPoints }

            ProChartModule.ChartType.AddressesCount ->
                marketKit.activeAddressesSingle(coinUid, currency.code, chartInterval, sessionKey)
                    .map { response -> response.countPoints }

            ProChartModule.ChartType.Tvl ->
                marketKit.marketInfoTvlSingle(coinUid, currency.code, chartInterval)
        }

        val isMovementChart = when (chartType) {
            ProChartModule.ChartType.DexLiquidity,
            ProChartModule.ChartType.Tvl -> true
            ProChartModule.ChartType.DexVolume,
            ProChartModule.ChartType.TxCount,
            ProChartModule.ChartType.AddressesCount -> false
        }

        return chartDataSingle
            .map { chartPoints ->
                val items = chartPoints.map { chartPoint ->
                    ChartPoint(
                        value = chartPoint.value.toFloat(),
                        timestamp = chartPoint.timestamp,
                        indicators = chartPoint.extra[ChartPointType.Volume]?.let { mapOf(Indicator.Volume to it.toFloat()) } ?: mapOf()
                    )
                }

                return@map ChartPointsWrapper(
                    chartInterval,
                    items,
                    null,
                    null,
                    false,
                    isMovementChart
                )
            }
    }
}
