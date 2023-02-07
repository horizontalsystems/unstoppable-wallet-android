package cash.p.terminal.entities.nft

import cash.p.terminal.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

data class NftKey(
    val account: Account,
    val blockchainType: BlockchainType
)