package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.modules.nft.INftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.NftCollection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class NftCollectionService(
    private val collectionUid: String,
    private val nftApiProvider: INftApiProvider
) {

    private val _nftCollection = MutableStateFlow<Result<NftCollection>?>(null)
    val nftCollection = _nftCollection.filterNotNull()

    var collection: NftCollection? = null
        private set

    suspend fun start() {
        val nftCollectionFetchResult = try {
            val collection = nftApiProvider.collection(collectionUid)
            this.collection = collection

            Result.success(collection)
        } catch (error: Exception) {
            Result.failure(error)
        }

        _nftCollection.emit(nftCollectionFetchResult)
    }


}
