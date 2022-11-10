package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.BlockchainType

class SolanaNftRecord(
        blockchainType: BlockchainType,
        val collectionAddress: String?,
        val tokenId: String,
        val tokenName: String?,
        balance: Int
) : NftRecord(blockchainType, balance) {

    override val nftUid: NftUid
        get() = NftUid.Solana(tokenId)

    override val displayName: String
        get() = tokenName ?: "#$tokenId"
}
