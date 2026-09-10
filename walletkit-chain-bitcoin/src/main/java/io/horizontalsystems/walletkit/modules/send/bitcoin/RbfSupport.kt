package io.horizontalsystems.walletkit.modules.send.bitcoin

import io.horizontalsystems.marketkit.models.BlockchainType

/** Replace-by-fee can be signalled on the chains whose nodes honour it. */
val BlockchainType.rbfSupported: Boolean
    get() = when (this) {
        BlockchainType.Bitcoin,
        BlockchainType.Litecoin -> true
        else -> false
    }
