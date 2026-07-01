package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

data class NftKey(
    val account: Account,
    val blockchainType: BlockchainType
)