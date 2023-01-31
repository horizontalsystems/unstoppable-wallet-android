package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftPrice
import java.math.BigDecimal
import java.util.*

data class NftCollectionMetadata(
    val blockchainType: BlockchainType,

    val providerUid: String,
    val contracts: List<NftContractMetadata>,

    val name: String,
    val description: String?,
    val imageUrl: String?,
    val thumbnailImageUrl: String?,
    val externalUrl: String?,
    val providerUrl: String?,
    val discordUrl: String?,
    val twitterUsername: String?,

    val count: Int?,
    val ownerCount: Int?,
    val totalSupply: Int?,
    val totalVolume: BigDecimal?,
    val floorPrice: NftPrice?,
    val marketCap: NftPrice?,

    val royalty: BigDecimal?,
    val inceptionDate: Date?,

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