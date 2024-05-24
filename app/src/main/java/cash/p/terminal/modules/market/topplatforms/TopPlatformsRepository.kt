package cash.p.terminal.modules.market.topplatforms

import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.modules.market.sortedByDescendingNullLast
import cash.p.terminal.modules.market.sortedByNullLast
import io.horizontalsystems.marketkit.models.TopPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

class TopPlatformsRepository(private val marketKit: MarketKitWrapper) {
    private var itemsCache: List<TopPlatform>? = null

    suspend fun get(
        sortingField: SortingField,
        timeDuration: TimeDuration,
        currencyCode: String,
        forceRefresh: Boolean,
        limit: Int? = null,
    ) = withContext(Dispatchers.IO) {
        val currentCache = itemsCache

        val items = if (forceRefresh || currentCache == null) {
            marketKit.topPlatformsSingle(currencyCode).await()
        } else {
            currentCache
        }

        itemsCache = items

        val topPlatformsByPeriod = getTopPlatformItems(items, timeDuration)

        topPlatformsByPeriod.sort(sortingField).let { sortedList ->
            limit?.let { sortedList.take(it) } ?: sortedList
        }
    }

    companion object {
        fun getTopPlatformItems(
                topPlatforms: List<TopPlatform>,
                timeDuration: TimeDuration
        ): List<TopPlatformItem> {
            return topPlatforms.map { platform ->
                val prevRank = when (timeDuration) {
                    TimeDuration.OneDay -> null
                    TimeDuration.SevenDay -> platform.rank1W
                    TimeDuration.ThirtyDay -> platform.rank1M
                    TimeDuration.ThreeMonths -> platform.rank3M
                }

                val rankDiff = if (prevRank == platform.rank || prevRank == null) {
                    null
                } else {
                    prevRank - platform.rank
                }

                val marketCapDiff = when (timeDuration) {
                    TimeDuration.OneDay -> null
                    TimeDuration.SevenDay -> platform.change1W
                    TimeDuration.ThirtyDay -> platform.change1M
                    TimeDuration.ThreeMonths -> platform.change3M
                }

                TopPlatformItem(
                        Platform(platform.blockchain.uid, platform.blockchain.name),
                        platform.rank,
                        platform.protocols,
                        platform.marketCap,
                        rankDiff,
                        marketCapDiff
                )
            }
        }
    }

    fun List<TopPlatformItem>.sort(sortingField: SortingField) = when (sortingField) {
        SortingField.HighestCap -> sortedByDescendingNullLast { it.marketCap }
        SortingField.LowestCap -> sortedByNullLast { it.marketCap }
        SortingField.TopGainers -> sortedByDescendingNullLast { it.changeDiff }
        SortingField.TopLosers -> sortedByNullLast { it.changeDiff }
        else -> this
    }

}
