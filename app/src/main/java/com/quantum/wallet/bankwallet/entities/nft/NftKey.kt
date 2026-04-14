package com.quantum.wallet.bankwallet.entities.nft

import com.quantum.wallet.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

data class NftKey(
    val account: Account,
    val blockchainType: BlockchainType
)