package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.marketkit.models.BlockchainType

object MultiSwapProviderRegistry {
    val allProviders: List<IMultiSwapProvider> = listOf(
        // Single-chain DEX providers
        OneInchProvider(),
//        UniswapProvider,
        UniswapV3Provider,
//        PancakeSwapProvider,
        PancakeSwapV3Provider,
//        QuickSwapProvider,
        // Cross-chain providers
        AllBridgeProvider,
        USwapProvider(UProvider.Near),
        USwapProvider(UProvider.QuickEx),
        USwapProvider(UProvider.LetsExchange),
        USwapProvider(UProvider.StealthEx),
        USwapProvider(UProvider.Exolix),
        USwapProvider(UProvider.Cce),
        USwapProvider(UProvider.Swapuz),
        USwapProvider(UProvider.Lizex),
        USwapProvider(UProvider.Bitania),
        USwapProvider(UProvider.Barter),
        USwapProvider(UProvider.Circle),
        USwapProvider(UProvider.Pegasus),
        USwapProvider(UProvider.Jupiter),
        USwapProvider(UProvider.Lifi),
        // Axelar ITS' Stellar leg is a server-built XDR envelope (signed_transaction), so
        // Stellar joins the default sourceAddress build signal (EVM covers the other leg).
        USwapProvider(
            UProvider.AxelarIts,
            shouldIncludeSourceAddress = { it.blockchainType.isEvm || it.blockchainType == BlockchainType.Stellar },
        ),
    ) + ChainRegistry.all.flatMap { it.swapProviders() }

    private val providersById: Map<String, IMultiSwapProvider> by lazy {
        allProviders.associateBy { it.id }
    }

    fun isSingleTransactionSwap(providerId: String, tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean {
        val provider = providersById[providerId] ?: return false
        return provider.isSingleTransactionSwap(tokenInBlockchainTypeUid, tokenOutBlockchainTypeUid)
    }

    fun providerType(providerId: String): SwapProviderType? {
        return providersById[providerId]?.type
    }
}
