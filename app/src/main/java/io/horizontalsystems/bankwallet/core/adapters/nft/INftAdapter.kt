package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.*
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftEvent
import io.horizontalsystems.marketkit.models.PagedNftEvents
import kotlinx.coroutines.flow.Flow


interface INftAdapter {
    val userAddress: String
    val nftRecordsFlow: Flow<List<NftRecord>>
    val nftRecords: List<NftRecord>
    fun sync()
}

interface INftProvider {
    val title: String
    val icon: Int
    suspend fun addressMetadata(blockchainType: BlockchainType, address: String): NftAddressMetadata
    suspend fun extendedAssetMetadata(nftUid: NftUid, providerCollectionUid: String): Pair<NftAssetMetadata, NftCollectionMetadata>
    suspend fun collectionMetadata(blockchainType: BlockchainType, providerUid: String): NftCollectionMetadata
    suspend fun assetEvents(contractAddress: String, tokenId: String, eventType: NftEvent.EventType?, cursor: String? = null): PagedNftEvents
}

/*
interface INftProvider {
    fun collectionLink(providerUid: String) -> ProviderLink?
    fun addressMetadataSingle(blockchainType: BlockchainType, address: String) -> Single<NftAddressMetadata>
    fun assetMetadataSingle(nftUid: NftUid) -> Single<NftAssetMetadata>
    fun collectionAssetsMetadataSingle(blockchainType: BlockchainType, providerCollectionUid: String, paginationData: PaginationData?) -> Single<([NftAssetMetadata], PaginationData?)>
    fun collectionMetadataSingle(blockchainType: BlockchainType, providerUid: String) -> Single<NftCollectionMetadata>
    fun assetEventsMetadataSingle(nftUid: NftUid, eventType: NftEventMetadata.EventType?, paginationData: PaginationData?) -> Single<([NftEventMetadata], PaginationData?)>
    fun collectionEventsMetadataSingle(blockchainType: BlockchainType, providerUid: String, eventType: NftEventMetadata.EventType?, paginationData: PaginationData?) -> Single<([NftEventMetadata], PaginationData?)>
}*/
