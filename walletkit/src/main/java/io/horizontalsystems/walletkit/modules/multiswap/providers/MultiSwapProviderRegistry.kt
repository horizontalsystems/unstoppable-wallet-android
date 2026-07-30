package io.horizontalsystems.walletkit.modules.multiswap.providers

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
        ThorChainProvider,
        MayaProvider,
        AllBridgeProvider,
        USwapProvider(UProvider.Near),
        USwapProvider(UProvider.QuickEx),
        USwapProvider(UProvider.LetsExchange),
        USwapProvider(UProvider.StealthEx),
        USwapProvider(UProvider.Exolix),
        USwapProvider(UProvider.Cce),
        USwapProvider(UProvider.Swapuz),
        USwapProvider(UProvider.Barter),
        USwapProvider(UProvider.Circle),
        USwapProvider(UProvider.Pegasus),
        USwapProvider(UProvider.Jupiter),
        USwapProvider(UProvider.Lifi),
        // Stellar-only pairs through uswap-server's Stellar venues (SOROSWAP/AQUARIUS/
        // STELLAR_DEX behind the single StellarBroker card) — not a USwapProvider because
        // it waterfalls across several server provider ids per quote.
        StellarSwapProvider(),
    )

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
