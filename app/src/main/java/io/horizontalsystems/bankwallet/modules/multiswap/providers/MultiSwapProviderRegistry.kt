package io.horizontalsystems.bankwallet.modules.multiswap.providers

object MultiSwapProviderRegistry {
    val allProviders: List<IMultiSwapProvider> = listOf(
        // Single-chain DEX providers
        OneInchProvider,
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
    )

    private val providersById: Map<String, IMultiSwapProvider> by lazy {
        allProviders.associateBy { it.id }
    }

    fun isSingleTransactionSwap(providerId: String, tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean {
        val provider = providersById[providerId] ?: return false
        return provider.isSingleTransactionSwap(tokenInBlockchainTypeUid, tokenOutBlockchainTypeUid)
    }

    fun isSingleTransactionEvmSwap(providerId: String, tokenInBlockchainTypeUid: String, tokenOutBlockchainTypeUid: String): Boolean {
        val provider = providersById[providerId] ?: return false
        return provider.isSingleTransactionSwap(tokenInBlockchainTypeUid, tokenOutBlockchainTypeUid) && provider.isEvm
    }
}
