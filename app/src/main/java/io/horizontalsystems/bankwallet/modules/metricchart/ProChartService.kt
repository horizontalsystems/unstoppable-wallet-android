package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import io.horizontalsystems.bankwallet.modules.profeatures.ProNft
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class ProChartService(
    override val currencyManager: ICurrencyManager,
    private val proFeaturesAuthorizationManager: ProFeaturesAuthorizationManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
    private val chartType: ProChartModule.ChartType
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1

    override val chartIntervals = HsTimePeriod.values().toList()

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
                    false
                )
            }
    }
}
