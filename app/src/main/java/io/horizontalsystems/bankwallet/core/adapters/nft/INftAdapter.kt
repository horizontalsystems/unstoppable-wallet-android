package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.Flow


interface INftAdapter {
    val userAddress: String
    val nftRecordsFlow: Flow<List<NftRecord>>
    val nftRecords: List<NftRecord>
    fun sync()
}

interface   INftProvider {
    suspend fun addressMetadata(blockchainType: BlockchainType, address: String): NftAddressMetadata
//    suspend fun assetInfo(providerCollectionUid: String, nftUid: NftUid): NftAssetInfo
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
