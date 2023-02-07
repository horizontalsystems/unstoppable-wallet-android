package cash.p.terminal.modules.nft.collection.overview

import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.providers.nft.INftProvider
import cash.p.terminal.entities.nft.NftCollectionMetadata
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NftCollectionOverviewService(
    val blockchainType: BlockchainType,
    val providerCollectionUid: String,
    private val provider: INftProvider,
    private val marketKit: MarketKitWrapper
) {
    private var fetchingJob: Job? = null

    private val _nftCollection = MutableStateFlow<Result<NftCollectionMetadata>?>(null)
    val nftCollection = _nftCollection.filterNotNull()

    val providerTitle = provider.title
    val providerIcon = provider.icon

    val blockchain: Blockchain?
        get() = marketKit.blockchain(blockchainType.uid)

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
                val collection = provider.collectionMetadata(blockchainType, providerCollectionUid)

                _nftCollection.emit(Result.success(collection))
            } catch (error: Exception) {
                _nftCollection.emit(Result.failure(error))
            }
        }
    }

}
