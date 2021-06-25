package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.subjects.PublishSubject

class SwapMainService(
        coinFrom: Coin?,
        private val providers: List<SwapMainModule.ISwapProvider>,
        private val localStorage: ILocalStorage
) {
    var dex: SwapMainModule.Dex = getDex(coinFrom)
        private set

    val currentProvider: SwapMainModule.ISwapProvider
        get() = dex.provider
    val providerObservable = PublishSubject.create<SwapMainModule.ISwapProvider>()

    var providerState = SwapMainModule.SwapProviderState(coinFrom = coinFrom)

    val availableProviders: List<SwapMainModule.ISwapProvider>
        get() = providers.filter { it.supports(dex.blockchain) }

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = SwapMainModule.Dex(dex.blockchain, provider)
            providerObservable.onNext(provider)

            localStorage.setSwapProviderId(dex.blockchain, provider.id)
        }
    }

    private fun getDex(coinFrom: Coin?): SwapMainModule.Dex {
        val blockchain = getBlockchainForCoin(coinFrom)
        val provider = getSwapProvider(blockchain)
                ?: throw IllegalStateException("No provider found for ${blockchain.title}")

        return SwapMainModule.Dex(blockchain, provider)
    }

    private fun getSwapProvider(blockchain: SwapMainModule.Blockchain): SwapMainModule.ISwapProvider? {
        val providerId = localStorage.getSwapProviderId(blockchain)
                ?: getDefaultSwapProviderId(blockchain)

        return providers.firstOrNull { it.id == providerId }
    }

    private fun getDefaultSwapProviderId(blockchain: SwapMainModule.Blockchain): String {
        return when (blockchain) {
            SwapMainModule.Blockchain.Ethereum,
            SwapMainModule.Blockchain.BinanceSmartChain -> SwapMainModule.OneInchProvider.id
        }
    }

    private fun getBlockchainForCoin(coin: Coin?): SwapMainModule.Blockchain {
        return when (coin?.type) {
            CoinType.Ethereum, is CoinType.Erc20, null -> SwapMainModule.Blockchain.Ethereum
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> SwapMainModule.Blockchain.BinanceSmartChain
            else -> throw IllegalStateException("Swap not supported for ${coin.type}")
        }
    }

}
