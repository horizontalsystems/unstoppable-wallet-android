package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class EvmBlockchainManager(
    private val marketKit: MarketKitWrapper,
) {

    val allBlockchains: List<Blockchain>
        get() = marketKit.blockchains(blockchainTypes.map { it.uid })

    val allMainNetBlockchains: List<Blockchain>
        get() = marketKit.blockchains(blockchainTypes.map { it.uid })

    /** EVM chain ids without depending on the kit's Chain enum. */
    fun getChainId(blockchainType: BlockchainType): Int? = when (blockchainType) {
        BlockchainType.Ethereum -> 1
        BlockchainType.BinanceSmartChain -> 56
        BlockchainType.Polygon -> 137
        BlockchainType.Avalanche -> 43114
        BlockchainType.Optimism -> 10
        BlockchainType.Base -> 8453
        BlockchainType.ZkSync -> 324
        BlockchainType.ArbitrumOne -> 42161
        BlockchainType.Gnosis -> 100
        BlockchainType.Fantom -> 250
        else -> null
    }

    fun getBlockchain(chainId: Int): Blockchain? =
        allBlockchains.firstOrNull { getChainId(it.type) == chainId }

    fun getBlockchain(token: Token): Blockchain? =
        allBlockchains.firstOrNull { token.blockchain == it }

    fun getBlockchain(blockchainType: BlockchainType): Blockchain? =
        allBlockchains.firstOrNull { it.type == blockchainType }

    fun getBaseToken(blockchainType: BlockchainType): Token? =
        marketKit.token(TokenQuery(blockchainType, TokenType.Native))

    companion object{
        val blockchainTypes = listOf(
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.Base,
            BlockchainType.ZkSync,
        )
    }
}
