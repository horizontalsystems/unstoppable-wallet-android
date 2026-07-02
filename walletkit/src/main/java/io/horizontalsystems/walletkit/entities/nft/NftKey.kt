package io.horizontalsystems.walletkit.entities.nft

import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

data class NftKey(
    val account: Account,
    val blockchainType: BlockchainType
)