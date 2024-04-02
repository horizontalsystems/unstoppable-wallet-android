package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.nft.INftProvider
import io.horizontalsystems.bankwallet.core.providers.nft.OpenSeaNftProvider
import io.horizontalsystems.bankwallet.core.storage.NftStorage
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetShortMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class NftMetadataManager(
    marketKit: MarketKitWrapper,
    appConfigProvider: AppConfigProvider,
    private val storage: NftStorage
) {
    private val providerMap by lazy {
        mapOf<BlockchainType, INftProvider>(
            BlockchainType.Ethereum to OpenSeaNftProvider(marketKit, appConfigProvider)
        )
    }

    private val _addressMetadataFlow = MutableSharedFlow<Pair<NftKey, NftAddressMetadata>?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val addressMetadataFlow: Flow<Pair<NftKey, NftAddressMetadata>?> = _addressMetadataFlow

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

    fun save(assetsBriefMetadata: List<NftAssetBriefMetadata>) {
        storage.save(assetsBriefMetadata)
    }

    fun assetShortMetadata(nftUid: NftUid) : NftAssetShortMetadata? {
        return storage.assetShortMetadata(nftUid)
    }

    fun assetsBriefMetadata(nftUids: Set<NftUid>): List<NftAssetBriefMetadata> {
        return storage.assetsBriefMetadata(nftUids)
    }

    suspend fun fetchAssetsBriefMetadata(nftUids: Set<NftUid>): List<NftAssetBriefMetadata> =
        nftUids.groupBy { it.blockchainType }
            .mapNotNull { (blockchainType, nftUids) ->
                providerMap[blockchainType]?.assetsBriefMetadata(blockchainType, nftUids)
            }
            .flatten()

    sealed class ProviderError(message: String?) : Throwable(message) {
        class NoProviderForBlockchainType(blockchainType: BlockchainType) : ProviderError("blockchainType: ${blockchainType.uid}")
    }

}