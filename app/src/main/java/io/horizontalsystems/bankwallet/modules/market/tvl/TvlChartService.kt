package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartPointsWrapper
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single

class TvlChartService(
    override val currencyManager: ICurrencyManager,
    private val globalMarketRepository: GlobalMarketRepository
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Day1

    override val chartIntervals = HsTimePeriod.values().toList()

    var chain: TvlModule.Chain = TvlModule.Chain.All
        set(value) {
            field = value
            dataInvalidated()
        }

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> {
        val chainParam = if (chain == TvlModule.Chain.All) "" else chain.name
        return globalMarketRepository.getTvlGlobalMarketPoints(
            chainParam,
            currency.code,
            chartInterval
        ).map {
            ChartPointsWrapper(chartInterval, it)
        }
    }
}
