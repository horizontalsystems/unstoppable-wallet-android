package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.modules.nft.INftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NftCollectionService(
    val collectionUid: String,
    private val nftApiProvider: INftApiProvider
) {
    private var fetchingJob: Job? = null

    private val _nftCollection = MutableStateFlow<Result<NftCollection>?>(null)
    val nftCollection = _nftCollection.filterNotNull()

    suspend fun start() {
        fetch()
    }

    suspend fun refresh() {
        fetch()
    }

    private suspend fun fetch() = withContext(Dispatchers.IO) {
        fetchingJob?.cancel()

        fetchingJob = launch {
            try {
                val collection = nftApiProvider.collection(collectionUid)

                _nftCollection.emit(Result.success(collection))
            } catch (error: Exception) {
                _nftCollection.emit(Result.failure(error))
            }
        }
    }

}
