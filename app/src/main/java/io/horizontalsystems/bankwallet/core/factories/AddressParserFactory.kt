package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.marketkit.models.BlockchainType

val BlockchainType.uriScheme: String?
    get() {
        if (EvmBlockchainManager.blockchainTypes.contains(this)) {
            return "ethereum"
        }

        return when (this) {
            BlockchainType.Bitcoin -> "bitcoin"
            BlockchainType.BitcoinCash -> "bitcoincash"
            BlockchainType.ECash -> "ecash"
            BlockchainType.Litecoin -> "litecoin"
            BlockchainType.Dash -> "dash"
            BlockchainType.Zcash -> "zcash"
            BlockchainType.Ethereum -> "ethereum"
            BlockchainType.Ton -> "toncoin"
            BlockchainType.Tron -> "tron"
            else -> null
        }
    }

val BlockchainType.removeScheme: Boolean
    get() {
        if (EvmBlockchainManager.blockchainTypes.contains(this)) {
            return true
        }

        return when (this) {
            BlockchainType.Bitcoin,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.Zcash,
            BlockchainType.Ethereum,
            BlockchainType.Ton,
            BlockchainType.Tron -> true

            else -> false
        }
    }