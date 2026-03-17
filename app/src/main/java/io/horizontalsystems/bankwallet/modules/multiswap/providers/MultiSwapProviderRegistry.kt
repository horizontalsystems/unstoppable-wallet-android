package io.horizontalsystems.bankwallet.modules.multiswap.providers

object MultiSwapProviderRegistry {
    val allProviders: List<IMultiSwapProvider> = listOf(
        // Single-chain DEX providers
        OneInchProvider,
//        UniswapProvider,
//        UniswapV3Provider,
//        PancakeSwapProvider,
//        PancakeSwapV3Provider,
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
        USwapProvider(UProvider.Swapuz),
    )

    private val providersById: Map<String, IMultiSwapProvider> by lazy {
        allProviders.associateBy { it.id }
    }

    fun isSingleChainSwap(providerId: String, tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean =
        providersById[providerId]?.isSingleChainSwap(tokenInBlockchainTypeUid, tokenOutBlockchainTypeUid) ?: false
}
