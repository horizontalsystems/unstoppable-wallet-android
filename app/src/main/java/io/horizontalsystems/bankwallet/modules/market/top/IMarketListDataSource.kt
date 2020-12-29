package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.Observable
import io.reactivex.Single

abstract class IMarketListDataSource {
    abstract val dataUpdatedAsync: Observable<Unit>
    abstract fun getListAsync(currencyCode: String, period: Period): Single<List<MarketTopItem>>

    protected fun convertToMarketTopItem(rank: Int, topMarket: TopMarket) =
            MarketTopItem(
                    rank,
                    topMarket.coin.code,
                    topMarket.coin.title,
                    topMarket.marketInfo.marketCap.toDouble(),
                    topMarket.marketInfo.volume.toDouble(),
                    topMarket.marketInfo.rate,
                    topMarket.marketInfo.rateDiffPeriod,
            )

    protected fun convertPeriod(period: Period) = when (period) {
        Period.Period24h -> TimePeriod.HOUR_24
        Period.PeriodWeek -> TimePeriod.DAY_7
        Period.PeriodMonth -> TimePeriod.DAY_30
    }

}

