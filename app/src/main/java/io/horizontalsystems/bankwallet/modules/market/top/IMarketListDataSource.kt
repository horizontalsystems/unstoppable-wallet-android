package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.Observable
import io.reactivex.Single

abstract class IMarketListDataSource {
    abstract val sortingFields: Array<Field>
    abstract val dataUpdatedAsync: Observable<Unit>

    fun getListAsync(currencyCode: String, period: Period, sortingField: Field): Single<List<MarketTopItem>> {
        return doGetListAsync(currencyCode, convertPeriod(period))
                .map {
                    sort(it.mapIndexed { index, topMarket ->
                        convertToMarketTopItem(index + 1, topMarket)
                    }, sortingField)
                }
    }

    protected abstract fun doGetListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<TopMarket>>

    private fun convertToMarketTopItem(rank: Int, topMarket: TopMarket) =
            MarketTopItem(
                    rank,
                    topMarket.coin.code,
                    topMarket.coin.title,
                    topMarket.marketInfo.marketCap.toDouble(),
                    topMarket.marketInfo.volume.toDouble(),
                    topMarket.marketInfo.rate,
                    topMarket.marketInfo.rateDiffPeriod,
                    topMarket.marketInfo.liquidity,
            )

    private fun convertPeriod(period: Period) = when (period) {
        Period.Period24h -> TimePeriod.HOUR_24
        Period.PeriodWeek -> TimePeriod.DAY_7
        Period.PeriodMonth -> TimePeriod.DAY_30
    }

    private fun sort(items: List<MarketTopItem>, sortingField: Field): List<MarketTopItem> {
        return when (sortingField) {
            Field.HighestCap -> items.sortedByDescending { it.marketCap }
            Field.LowestCap -> items.sortedBy { it.marketCap }
            Field.HighestLiquidity -> items.sortedByDescending { it.liquidity }
            Field.LowestLiquidity -> items.sortedBy { it.liquidity }
            Field.HighestVolume -> items.sortedByDescending { it.volume }
            Field.LowestVolume -> items.sortedBy { it.volume }
            Field.HighestPrice -> items.sortedByDescending { it.rate }
            Field.LowestPrice -> items.sortedBy { it.rate }
            Field.TopGainers -> items.sortedByDescending { it.diff }
            Field.TopLosers -> items.sortedBy { it.diff }
        }
    }

}

