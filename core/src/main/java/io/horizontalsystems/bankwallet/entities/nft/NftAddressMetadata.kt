package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.NftPrice

data class NftAddressMetadata(
    val collections: List<NftCollectionShortMetadata>,
    val assets: List<NftAssetShortMetadata>
)

data class NftCollectionShortMetadata(
    val providerUid: String,
    val name: String,
    val thumbnailImageUrl: String?,
    val averagePrice7d: NftPrice?,
    val averagePrice30: NftPrice?
)

data class NftAssetShortMetadata(
    val nftUid: NftUid,
    val providerCollectionUid: String,
    val name: String?,
    val previewImageUrl: String?,
    val onSale: Boolean,
    val lastSalePrice: NftPrice?
) {
    val displayName = name ?: "#${nftUid.tokenId}"
}