package io.horizontalsystems.bankwallet.modules.market.topnftcollections

import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class TopNftCollectionsService(
    sortingField: SortingField,
    timeDuration: TimeDuration,
    private val topNftCollectionsRepository: TopNftCollectionsRepository
) {
    private var topNftsJob: Job? = null

    private val _nftCollectionsItem = MutableStateFlow<Result<List<NftCollectionItem>>?>(null)
    val topNftCollections = _nftCollectionsItem.filterNotNull()

    val sortingFields = listOf(
        SortingField.HighestVolume,
        SortingField.LowestVolume,
        SortingField.TopGainers,
        SortingField.TopLosers
    )
    var sortingField: SortingField = sortingField
        private set

    val timeDurations = TimeDuration.values().toList()
    var timeDuration: TimeDuration = timeDuration
        private set

    suspend fun start() {
        update(true)
    }

    suspend fun refresh() {
        update(true)
    }

    private suspend fun update(forceRefresh: Boolean) = withContext(Dispatchers.IO) {
        topNftsJob?.cancel()

        topNftsJob = launch {
            try {
                val topNfts = topNftCollectionsRepository.get(
                    sortingField = sortingField,
                    timeDuration = timeDuration,
                    forceRefresh = forceRefresh,
                    limit = 100
                )
                _nftCollectionsItem.emit(Result.success(topNfts))
            } catch (cancellation: CancellationException) {
                // do nothing
            } catch (error: Exception) {
                _nftCollectionsItem.emit(Result.failure(error))
            }
        }
    }

    suspend fun setSortingField(sortingField: SortingField) {
        this.sortingField = sortingField

        update(false)
    }

    suspend fun setTimeDuration(timeDuration: TimeDuration) {
        this.timeDuration = timeDuration

        update(false)
    }

}
