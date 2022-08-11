package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.factories.EvmAccountManagerFactory
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.models.*

class EvmBlockchainManager(
    private val backgroundManager: BackgroundManager,
    private val syncSourceManager: EvmSyncSourceManager,
    private val marketKit: MarketKitWrapper,
    private val accountManagerFactory: EvmAccountManagerFactory
) {
    private val evmKitManagersMap = mutableMapOf<BlockchainType, Pair<EvmKitManager, EvmAccountManager>>()

    val allBlockchainTypes = listOf(
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne,
    )

    val allBlockchains = marketKit.blockchains(allBlockchainTypes.map { it.uid })

    private fun getEvmKitManagers(blockchainType: BlockchainType): Pair<EvmKitManager, EvmAccountManager> {
        val evmKitManagers = evmKitManagersMap[blockchainType]

        evmKitManagers?.let {
            return it
        }

        val evmKitManager = EvmKitManager(getChain(blockchainType), backgroundManager, syncSourceManager)
        val evmAccountManager = accountManagerFactory.evmAccountManager(blockchainType, evmKitManager)

        val pair = Pair(evmKitManager, evmAccountManager)

        evmKitManagersMap[blockchainType] = pair

        return pair
    }

    fun getChain(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Ethereum -> Chain.Ethereum
        BlockchainType.BinanceSmartChain -> Chain.BinanceSmartChain
        BlockchainType.Polygon -> Chain.Polygon
        BlockchainType.Avalanche -> Chain.Avalanche
        BlockchainType.Optimism -> Chain.Optimism
        BlockchainType.ArbitrumOne -> Chain.ArbitrumOne
        else -> throw IllegalArgumentException("Unsupported blockchain type $blockchainType")
    }

    fun getBlockchain(chainId: Int): Blockchain? =
        allBlockchains.firstOrNull { getChain(it.type).id == chainId }

    fun getBlockchain(token: Token): Blockchain? =
        allBlockchains.firstOrNull { token.blockchain == it }

    fun getBlockchain(blockchainType: BlockchainType): Blockchain? =
        allBlockchains.firstOrNull { it.type == blockchainType }

    fun getEvmKitManager(blockchainType: BlockchainType): EvmKitManager =
        getEvmKitManagers(blockchainType).first

    fun getEvmAccountManager(blockchainType: BlockchainType): EvmAccountManager =
        getEvmKitManagers(blockchainType).second

    fun getBaseToken(blockchainType: BlockchainType): Token? =
        marketKit.token(TokenQuery(blockchainType, TokenType.Native))

}
