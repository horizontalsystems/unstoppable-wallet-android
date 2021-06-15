package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.Blockchain
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.Dex
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType

class SwapMainViewModel(
        coinFrom: Coin?,
        val swapProviders: List<ISwapProvider>
) : ViewModel() {


    var dex: Dex = getDefaultDex(coinFrom)
        private set

    val provider: ISwapProvider
        get() = dex.provider
    val providerLiveData = MutableLiveData<ISwapProvider>()

    var providerState = SwapMainModule.SwapProviderState(coinFrom = coinFrom)

    fun setProvider(provider: ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = Dex(dex.blockchain, provider)
            providerLiveData.postValue(provider)
        }
    }

    private fun getDefaultDex(coinFrom: Coin?): Dex {
        val blockchain = when (coinFrom?.type) {
            CoinType.Ethereum, is CoinType.Erc20, null -> Blockchain.Ethereum
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> Blockchain.BinanceSmartChain
            else -> throw IllegalStateException("Swap not supported for ${coinFrom.type}")
        }

        val provider = getSwapDefaultProvider(blockchain)
                ?: throw IllegalStateException("No provider found for ${blockchain.title}")

        return Dex(blockchain, provider)
    }

    private fun getSwapDefaultProvider(blockchain: Blockchain): ISwapProvider? {
        // TODO get default provider from local storage
        val providerId = when (blockchain) {
            Blockchain.Ethereum -> "uniswap"
            Blockchain.BinanceSmartChain -> "uniswap"
        }
        return swapProviders.firstOrNull { it.id == providerId }
    }

}
