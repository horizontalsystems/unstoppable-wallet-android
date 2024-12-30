package cash.p.terminal.entities.nft

import io.horizontalsystems.core.entities.BlockchainType

data class NftKey(
    val account: cash.p.terminal.wallet.Account,
    val blockchainType: BlockchainType
)