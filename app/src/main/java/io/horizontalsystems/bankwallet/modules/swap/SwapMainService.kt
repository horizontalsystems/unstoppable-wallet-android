package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.subjects.PublishSubject

class SwapMainService(
        coinFrom: Coin?,
        private val providers: List<SwapMainModule.ISwapProvider>
) {
    var dex: SwapMainModule.Dex = getDefaultDex(coinFrom)
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
        }
    }

    private fun getDefaultDex(coinFrom: Coin?): SwapMainModule.Dex {
        val blockchain = when (coinFrom?.type) {
            CoinType.Ethereum, is CoinType.Erc20, null -> SwapMainModule.Blockchain.Ethereum
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> SwapMainModule.Blockchain.BinanceSmartChain
            else -> throw IllegalStateException("Swap not supported for ${coinFrom.type}")
        }

        val provider = getSwapDefaultProvider(blockchain)
                ?: throw IllegalStateException("No provider found for ${blockchain.title}")

        return SwapMainModule.Dex(blockchain, provider)
    }

    private fun getSwapDefaultProvider(blockchain: SwapMainModule.Blockchain): SwapMainModule.ISwapProvider? {
        // TODO get default provider from local storage
        val providerId = when (blockchain) {
            SwapMainModule.Blockchain.Ethereum -> "uniswap"
            SwapMainModule.Blockchain.BinanceSmartChain -> "uniswap"
        }
        return providers.firstOrNull { it.id == providerId }
    }
}