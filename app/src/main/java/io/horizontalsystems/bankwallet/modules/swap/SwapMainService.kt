package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.subjects.PublishSubject

class SwapMainService(
    tokenFrom: Token?,
    private val providers: List<SwapMainModule.ISwapProvider>,
    private val localStorage: ILocalStorage
) {
    var dex: SwapMainModule.Dex = getDex(tokenFrom)
        private set

    val currentProvider: SwapMainModule.ISwapProvider
        get() = dex.provider
    val providerObservable = PublishSubject.create<SwapMainModule.ISwapProvider>()

    var providerState = SwapMainModule.SwapProviderState(tokenFrom = tokenFrom)

    val availableProviders: List<SwapMainModule.ISwapProvider>
        get() = providers.filter { it.supports(dex.blockchainType) }

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = SwapMainModule.Dex(dex.blockchain, provider)
            providerObservable.onNext(provider)

            localStorage.setSwapProviderId(dex.blockchainType, provider.id)
        }
    }

    private fun getDex(tokenFrom: Token?): SwapMainModule.Dex {
        val blockchain = getBlockchainForToken(tokenFrom)
        val provider = getSwapProvider(blockchain.type)
            ?: throw IllegalStateException("No provider found for ${blockchain}")

        return SwapMainModule.Dex(blockchain, provider)
    }

    private fun getSwapProvider(blockchainType: BlockchainType): SwapMainModule.ISwapProvider? {
        val providerId = localStorage.getSwapProviderId(blockchainType)
            ?: SwapMainModule.OneInchProvider.id

        return providers.firstOrNull { it.id == providerId }
    }

    private fun getBlockchainForToken(token: Token?) = when (token?.blockchainType) {
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne -> token.blockchain
        null -> Blockchain(BlockchainType.Ethereum, "Ethereum", null) // todo: find better solution
        else -> throw IllegalStateException("Swap not supported for ${token.blockchainType}")
    }

}
