package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.xxxkit.models.BlockchainType
import io.horizontalsystems.xxxkit.models.Token
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
        get() = providers.filter { it.supports(dex.blockchain) }

    val blockchainTitle: String
        get() = dex.blockchain.name

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = SwapMainModule.Dex(dex.blockchain, provider)
            providerObservable.onNext(provider)

            localStorage.setSwapProviderId(dex.blockchain, provider.id)
        }
    }

    private fun getDex(coinFrom: Token?): SwapMainModule.Dex {
        val blockchain = getBlockchainForCoin(coinFrom)
        val provider = getSwapProvider(blockchain)
            ?: throw IllegalStateException("No provider found for ${blockchain.name}")

        return SwapMainModule.Dex(blockchain, provider)
    }

    private fun getSwapProvider(blockchain: EvmBlockchain): SwapMainModule.ISwapProvider? {
        val providerId = localStorage.getSwapProviderId(blockchain)
            ?: SwapMainModule.OneInchProvider.id

        return providers.firstOrNull { it.id == providerId }
    }

    private fun getBlockchainForCoin(coin: Token?) = when (coin?.blockchainType) {
        BlockchainType.Ethereum -> EvmBlockchain.Ethereum
        BlockchainType.BinanceSmartChain -> EvmBlockchain.BinanceSmartChain
        BlockchainType.Polygon -> EvmBlockchain.Polygon
        BlockchainType.Optimism -> EvmBlockchain.Optimism
        BlockchainType.ArbitrumOne -> EvmBlockchain.ArbitrumOne
        null -> EvmBlockchain.Ethereum
        else -> throw IllegalStateException("Swap not supported for ${coin.blockchainType}")
    }

}
