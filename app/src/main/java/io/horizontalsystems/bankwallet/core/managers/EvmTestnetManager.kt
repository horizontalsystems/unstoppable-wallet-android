package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.marketkit.models.*

class EvmTestnetManager {
    val allBlockchainTypes = listOf(
        BlockchainType.EthereumGoerli
    )

    val allBlockchains = allBlockchainTypes.mapNotNull { blockchain(it) }
    val nativeTokens = allBlockchainTypes.mapNotNull { getNativeToken(it) }

    fun getNativeToken(type: BlockchainType): Token? {
        val blockchain = blockchain(type) ?: return null

        return when (type) {
            BlockchainType.EthereumGoerli -> {
                Token(Coin("ethereum-goerli", "Ethereum Goerli", "GoerliETH"), blockchain, TokenType.Native, 18)
            }
            else -> null
        }
    }

    fun blockchain(type: BlockchainType): Blockchain? {
        return when (type) {
            BlockchainType.EthereumGoerli -> Blockchain(type, "Ethereum Goerli", null)
            else -> null
        }
    }
}
