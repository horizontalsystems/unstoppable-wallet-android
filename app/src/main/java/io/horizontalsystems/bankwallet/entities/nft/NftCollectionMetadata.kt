package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftPrice
import java.math.BigDecimal

data class NftCollectionMetadata(
    val blockchainType: BlockchainType,

    val providerUid: String,
    val contracts: List<String>,

    val name: String,
    val description: String?,
    val imageUrl: String?,
    val thumbnailImageUrl: String?,
    val externalUrl: String?,
    val discordUrl: String?,
    val twitterUserName: String?,

    val count: Int?,
    val ownerCount: Int?,
    val totalSupply: Int,
    val totalVolume: BigDecimal?,
    val floorPrice: NftPrice?,
    val marketCap: NftPrice?,

    val stats1d: Stats?,
    val stats7d: Stats?,
    val stats30d: Stats?
) {
    data class Stats(
        val volume: NftPrice?,
        val change: BigDecimal?,
        val sales: Int?,
        val averagePrice: NftPrice?
    )
}