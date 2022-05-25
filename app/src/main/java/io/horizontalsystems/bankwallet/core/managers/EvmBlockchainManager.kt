package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.factories.EvmAccountManagerFactory
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

class EvmBlockchainManager(
    private val backgroundManager: BackgroundManager,
    private val syncSourceManager: EvmSyncSourceManager,
    private val marketKit: MarketKit,
    private val accountManagerFactory: EvmAccountManagerFactory
) {
    private var evmKitManagersMap: MutableMap<EvmBlockchain, Pair<EvmKitManager, EvmAccountManager>> = mutableMapOf()

    private fun getEvmKitManagers(blockchain: EvmBlockchain): Pair<EvmKitManager, EvmAccountManager> {
        val evmKitManagers = evmKitManagersMap[blockchain]

        evmKitManagers?.let {
            return it
        }

        val evmKitManager = EvmKitManager(getChain(blockchain), backgroundManager, syncSourceManager)
        val evmAccountManager = accountManagerFactory.evmAccountManager(blockchain, evmKitManager)

        val pair = Pair(evmKitManager, evmAccountManager)

        evmKitManagersMap[blockchain] = pair

        return pair
    }

    fun getChain(blockchain: EvmBlockchain) =
        when (blockchain) {
            EvmBlockchain.Ethereum -> Chain.Ethereum
            EvmBlockchain.BinanceSmartChain -> Chain.BinanceSmartChain
            EvmBlockchain.Polygon -> Chain.Polygon
            EvmBlockchain.Optimism -> Chain.Optimism
            EvmBlockchain.ArbitrumOne -> Chain.ArbitrumOne
        }

    fun getBlockchain(chainId: Int): EvmBlockchain? =
        allBlockchains.firstOrNull { getChain(it).id == chainId }

    fun getBlockchain(coinType: CoinType): EvmBlockchain? =
        allBlockchains.firstOrNull { it.supports(coinType) }

    fun getEvmKitManager(blockchain: EvmBlockchain): EvmKitManager =
        getEvmKitManagers(blockchain).first

    fun getEvmAccountManager(blockchain: EvmBlockchain): EvmAccountManager =
        getEvmKitManagers(blockchain).second

    fun getBasePlatformCoin(blockchain: EvmBlockchain): PlatformCoin? =
        marketKit.platformCoin(blockchain.baseCoinType)

    val allBlockchains: List<EvmBlockchain> = listOf(
        EvmBlockchain.Ethereum,
        EvmBlockchain.BinanceSmartChain,
        EvmBlockchain.Polygon,
//        EvmBlockchain.Optimism,
//        EvmBlockchain.ArbitrumOne,
    )

}
