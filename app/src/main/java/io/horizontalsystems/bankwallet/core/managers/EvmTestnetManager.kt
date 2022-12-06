package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.*
import io.reactivex.subjects.PublishSubject

class EvmTestnetManager(private val localStorage: ILocalStorage) {
    val allBlockchainTypes = listOf(BlockchainType.EthereumGoerli)
    val testnetUpdatedSignal = PublishSubject.create<Boolean>()

    var isTestnetEnabled: Boolean
        get() = localStorage.testnetEnabled
        set(enabled) {
            localStorage.testnetEnabled = enabled
            testnetUpdatedSignal.onNext(enabled)
        }

    fun getNativeToken(type: BlockchainType): Token? {
        val blockchain = blockchain(type) ?: return null

        return when (type) {
            BlockchainType.EthereumGoerli -> {
                Token(Coin("ethereum-goerli", "Ethereum Goerli", "GoerliETH"), blockchain, TokenType.Native, 18)
            }
            else -> null
        }
    }

    fun tokens(queries: List<TokenQuery>): List<Token> {
        if (queries.isEmpty()) {
            return emptyList()
        }

        return queries.mapNotNull {
            if (it.tokenType !== TokenType.Native) {
                null
            } else {
                getNativeToken(it.blockchainType)
            }
        }
    }

    fun nativeTokens(filter: String? = null): List<Token> {
        if (localStorage.testnetEnabled) {
            var tokens = allBlockchainTypes.mapNotNull { getNativeToken(it) }
            val filterText = filter?.lowercase()
            if (filterText != null) {
                tokens = tokens.filter {
                    it.coin.name.lowercase().contains(filterText) || it.coin.code.lowercase().contains(filterText)
                }
            }
            return tokens
        }

        return listOf()
    }

    fun blockchains(): List<Blockchain> {
        if (localStorage.testnetEnabled) {
            return allBlockchainTypes.mapNotNull { blockchain(it) }
        }

        return listOf()
    }

    fun blockchain(type: BlockchainType): Blockchain? {
        return when (type) {
            BlockchainType.EthereumGoerli -> Blockchain(type, "Ethereum Goerli", null)
            else -> null
        }
    }
}
