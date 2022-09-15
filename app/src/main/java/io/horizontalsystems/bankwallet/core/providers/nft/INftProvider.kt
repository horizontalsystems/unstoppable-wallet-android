package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.entities.nft.*
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftEvent

interface INftProvider {
    val title: String
    val icon: Int
    suspend fun addressMetadata(blockchainType: BlockchainType, address: String): NftAddressMetadata
    suspend fun extendedAssetMetadata(nftUid: NftUid, providerCollectionUid: String): Pair<NftAssetMetadata, NftCollectionMetadata>
    suspend fun collectionMetadata(blockchainType: BlockchainType, providerUid: String): NftCollectionMetadata
    suspend fun collectionAssetsMetadata(
        blockchainType: BlockchainType,
        providerUid: String,
        paginationData: PaginationData?
    ): Pair<List<NftAssetMetadata>, PaginationData?>

    suspend fun collectionEventsMetadata(
        blockchainType: BlockchainType,
        providerUid: String,
        eventType: NftEvent.EventType?,
        paginationData: PaginationData?
    ): Pair<List<NftEventMetadata>, PaginationData?>

    suspend fun assetEventsMetadata(
        nftUid: NftUid,
        eventType: NftEvent.EventType?,
        paginationData: PaginationData?
    ): Pair<List<NftEventMetadata>, PaginationData?>
}

sealed class PaginationData {
    class Cursor(val value: String) : PaginationData()
    class Page(val value: Int) : PaginationData()

    val cursor: String?
        get() = when (this) {
            is Cursor -> value
            is Page -> null
        }

    val page: Int?
        get() = when (this) {
            is Cursor -> null
            is Page -> value
        }
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
