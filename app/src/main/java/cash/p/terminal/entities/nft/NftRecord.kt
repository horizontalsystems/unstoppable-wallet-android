package cash.p.terminal.entities.nft

import io.horizontalsystems.core.entities.BlockchainType

abstract class NftRecord(
    val blockchainType: BlockchainType,
    val balance: Int
) {
    abstract val nftUid: NftUid
    abstract val displayName: String
}