package cash.p.terminal.core.factories

import cash.p.terminal.core.managers.EvmBlockchainManager
import io.horizontalsystems.core.entities.BlockchainType

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
            BlockchainType.Dogecoin -> "dogecoin"
            BlockchainType.Dash -> "dash"
            BlockchainType.Zcash -> "zcash"
            BlockchainType.Ethereum -> "ethereum"
            BlockchainType.Ton -> "toncoin"
            BlockchainType.Tron -> "tron"
            BlockchainType.Stellar -> "stellar"
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
            BlockchainType.Dogecoin,
            BlockchainType.Dash,
            BlockchainType.Zcash,
            BlockchainType.Ethereum,
            BlockchainType.Ton,
            BlockchainType.Stellar,
            BlockchainType.Tron -> true

            else -> false
        }
    }