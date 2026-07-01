package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.BlockchainType

abstract class NftRecord(
    val blockchainType: BlockchainType,
    val balance: Int
) {
    abstract val nftUid: NftUid
    abstract val displayName: String
}