package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.nft.INftProvider
import io.horizontalsystems.bankwallet.core.providers.nft.OpenSeaNftProvider
import io.horizontalsystems.bankwallet.core.storage.NftStorage
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

class NftMetadataManager(
    marketKit: MarketKitWrapper,
    appConfigProvider: AppConfigProvider,
    private val storage: NftStorage
) {
    private val providerMap = mapOf<BlockchainType, INftProvider>(
        BlockchainType.Ethereum to OpenSeaNftProvider(marketKit, appConfigProvider)
    )

    private val _addressMetadataFlow = MutableStateFlow<Pair<NftKey, NftAddressMetadata>?>(null)
    val addressMetadataFlow: Flow<Pair<NftKey, NftAddressMetadata>> = _addressMetadataFlow.filterNotNull()

    suspend fun addressMetadata(blockchainType: BlockchainType, address: String): NftAddressMetadata {
        return provider(blockchainType).addressMetadata(blockchainType, address)
    }

    fun addressMetadata(nftKey: NftKey): NftAddressMetadata? {
        return storage.addressInfo(nftKey)
    }

    fun handle(nftAddressMetadata: NftAddressMetadata, nftKey: NftKey) {
        storage.save(nftAddressMetadata, nftKey)
        _addressMetadataFlow.tryEmit(Pair(nftKey, nftAddressMetadata))
    }

    fun provider(blockchainType: BlockchainType): INftProvider {
        return providerMap[blockchainType] ?: throw ProviderError.NoProviderForBlockchainType(blockchainType)
    }

    sealed class ProviderError(message: String?) : Throwable(message) {
        class NoProviderForBlockchainType(blockchainType: BlockchainType) : ProviderError("blockchainType: ${blockchainType.uid}")
    }

}