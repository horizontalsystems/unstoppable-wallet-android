package com.quantum.wallet.bankwallet.modules.metricchart

import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import com.quantum.wallet.bankwallet.entities.Currency
import com.quantum.wallet.bankwallet.modules.chart.AbstractChartService
import com.quantum.wallet.bankwallet.modules.chart.ChartPointsWrapper
import com.quantum.wallet.chartview.ChartViewType
import com.quantum.wallet.chartview.models.ChartPoint
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class CoinTvlChartService(
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val coinUid: String,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Month1
    override val chartIntervals = HsTimePeriod.values().toList()
    override val chartViewType = ChartViewType.Line

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> = try {
        marketKit.marketInfoTvlSingle(coinUid, currency.code, chartInterval)
            .map { info ->
                info.map { ChartPoint(it.value.toFloat(), it.timestamp) }
            }
            .map { ChartPointsWrapper(it) }
    } catch (e: Exception) {
        Single.error(e)
    }

}
