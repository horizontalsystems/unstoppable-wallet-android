package io.horizontalsystems.core.entities.nft

import io.horizontalsystems.core.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

data class NftKey(
    val account: Account,
    val blockchainType: BlockchainType
)