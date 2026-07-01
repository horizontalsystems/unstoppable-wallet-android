package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.NftPrice
import java.util.*

data class NftAssetMetadata(
    val nftUid: NftUid,
    val providerCollectionUid: String,

    val name: String?,
    val imageUrl: String?,
    val previewImageUrl: String?,
    val description: String?,
    val nftType: String?,
    val externalLink: String?,
    val providerLink: String?,

    val traits: List<Trait>,
    val lastSalePrice: NftPrice?,
    val offers: List<NftPrice>,
    val saleInfo: SaleInfo?
) {
    val displayName = name ?: "#${nftUid.tokenId}"

    data class Trait(
        val type: String,
        val value: String,
        val count: Int,
        val searchUrl: String?
    )

    data class SaleInfo(
        val type: SaleType,
        val listings: List<SaleListing>
    )

    enum class SaleType {
        OnSale, OnAuction
    }

    data class SaleListing(
        val untilDate: Date,
        val price: NftPrice
    )
}