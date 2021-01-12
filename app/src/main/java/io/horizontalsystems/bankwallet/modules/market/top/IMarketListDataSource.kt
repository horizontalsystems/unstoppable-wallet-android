package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*

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

    protected abstract fun doGetListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarket>>

    private fun convertToMarketTopItem(rank: Int, topMarket: CoinMarket) =
            MarketTopItem(
                    rank,
                    topMarket.coin.code,
                    topMarket.coin.title,
                    topMarket.marketInfo.volume.toDouble(),
                    topMarket.marketInfo.rate,
                    topMarket.marketInfo.rateDiffPeriod,
                    topMarket.marketInfo.marketCap?.toDouble(),
                    topMarket.marketInfo.liquidity,
            )

    private fun convertPeriod(period: Period) = when (period) {
        Period.Period24h -> TimePeriod.HOUR_24
        Period.PeriodWeek -> TimePeriod.DAY_7
        Period.PeriodMonth -> TimePeriod.DAY_30
    }

    private fun sort(items: List<MarketTopItem>, sortingField: Field): List<MarketTopItem> {
        return when (sortingField) {
            Field.HighestCap -> items.sortedByDescendingNullLast { it.marketCap }
            Field.LowestCap -> items.sortedByNullLast { it.marketCap }
            Field.HighestLiquidity -> items.sortedByDescendingNullLast { it.liquidity }
            Field.LowestLiquidity -> items.sortedByNullLast { it.liquidity }
            Field.HighestVolume -> items.sortedByDescendingNullLast { it.volume }
            Field.LowestVolume -> items.sortedByNullLast { it.volume }
            Field.HighestPrice -> items.sortedByDescendingNullLast { it.rate }
            Field.LowestPrice -> items.sortedByNullLast { it.rate }
            Field.TopGainers -> items.sortedByDescendingNullLast { it.diff }
            Field.TopLosers -> items.sortedByNullLast { it.diff }
        }
    }

}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByDescendingNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(Comparator.nullsLast(compareByDescending(selector)))
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(Comparator.nullsLast(compareBy(selector)))
}
