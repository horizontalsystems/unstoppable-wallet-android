package cash.p.terminal.modules.metricchart

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.chart.AbstractChartService
import cash.p.terminal.modules.chart.ChartPointsWrapper
import cash.p.terminal.modules.profeatures.ProFeaturesAuthorizationManager
import cash.p.terminal.modules.profeatures.ProNft
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
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

    override val chartIntervals = HsTimePeriod.values().toList()
    override val chartViewType = ChartViewType.Line

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

            ProChartModule.ChartType.TxVolume ->
                marketKit.transactionDataSingle(coinUid, currency.code, chartInterval, null, sessionKey)
                    .map { response -> response.volumePoints }

            ProChartModule.ChartType.AddressesCount ->
                marketKit.activeAddressesSingle(coinUid, currency.code, chartInterval, sessionKey)
                    .map { response -> response.countPoints }
        }

        val isMovementChart = when (chartType) {
            ProChartModule.ChartType.DexLiquidity,
            ProChartModule.ChartType.AddressesCount -> true
            ProChartModule.ChartType.DexVolume,
            ProChartModule.ChartType.TxCount,
            ProChartModule.ChartType.TxVolume -> false
        }

        return chartDataSingle
            .map { chartPoints ->
                val items = chartPoints.mapNotNull { chartPoint ->
                    ChartPoint(chartPoint.value.toFloat(), chartPoint.timestamp)
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
