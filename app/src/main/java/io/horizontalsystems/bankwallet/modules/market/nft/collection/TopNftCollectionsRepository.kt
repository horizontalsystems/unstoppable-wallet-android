package io.horizontalsystems.bankwallet.modules.market.nft.collection

import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.bankwallet.modules.nft.INftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.TopNftCollection

class TopNftCollectionsRepository(
    private val nftApiProvider: INftApiProvider
) {
    private val maxItemsCount = 1500
    private var itemsCache: List<TopNftCollection>? = null


    suspend fun get(
        limit: Int,
        sortingField: SortingField,
        timeDuration: TimeDuration,
        forceRefresh: Boolean
    ): List<TopNftCollection> {
        val currentCache = itemsCache

        val items = if (forceRefresh || currentCache == null) {
            nftApiProvider.topCollections(maxItemsCount)
        } else {
            currentCache
        }

        itemsCache = items

        return items.sort(sortingField, timeDuration).take(limit)
    }

    private fun List<TopNftCollection>.sort(sortingField: SortingField, timeDuration: TimeDuration) =
        when (sortingField) {
            SortingField.HighestCap,
            SortingField.LowestCap -> this
            SortingField.HighestVolume -> sortedByDescendingNullLast { it.volume(timeDuration) }
            SortingField.LowestVolume -> sortedByNullLast { it.volume(timeDuration) }
            SortingField.TopGainers -> sortedByDescendingNullLast { it.volumeDiff(timeDuration) }
            SortingField.TopLosers -> sortedByNullLast { it.volumeDiff(timeDuration) }
        }

    private fun TopNftCollection.volume(timeDuration: TimeDuration) =
        when (timeDuration) {
            TimeDuration.OneDay -> oneDayVolume
            TimeDuration.SevenDays -> sevenDaysVolume
            TimeDuration.ThirtyDays -> thirtyDaysVolume
        }

    private fun TopNftCollection.volumeDiff(timeDuration: TimeDuration) =
        when (timeDuration) {
            TimeDuration.OneDay -> oneDayVolumeDiff
            TimeDuration.SevenDays -> sevenDaysVolumeDiff
            TimeDuration.ThirtyDays -> thirtyDaysVolumeDiff
        }

}
