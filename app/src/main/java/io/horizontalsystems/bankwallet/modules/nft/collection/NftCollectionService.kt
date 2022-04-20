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


    suspend fun start() {
        val nftCollectionFetchResult = try {
            Result.success(nftApiProvider.collection(collectionUid))
        } catch (error: Exception) {
            Result.failure(error)
        }

        _nftCollection.emit(nftCollectionFetchResult)
    }


}
