package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views.FullTransactionBinanceAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views.FullTransactionBitcoinAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views.FullTransactionEosAdapter
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views.FullTransactionEthereumAdapter

class FullTransactionInfoFactory(private val networkManager: INetworkManager, private val dataProviderManager: ITransactionDataProviderManager)
    : FullTransactionInfoModule.ProviderFactory {

    override fun providerFor(coin: Coin): FullTransactionInfoModule.FullProvider {
        val baseProvider = dataProviderManager.baseProvider(coin)

        val provider: FullTransactionInfoModule.Provider
        val adapter: FullTransactionInfoModule.Adapter

        when {
            // BTC, BTCt
            coin.type is CoinType.Bitcoin -> {
                val providerBTC = dataProviderManager.bitcoin(baseProvider.name)

                provider = providerBTC
                adapter = FullTransactionBitcoinAdapter(providerBTC, coin, "satoshi")
            }
            // BCH, BCHt
            coin.type is CoinType.BitcoinCash -> {
                val providerBCH = dataProviderManager.bitcoinCash(baseProvider.name)

                provider = providerBCH
                adapter = FullTransactionBitcoinAdapter(providerBCH, coin, "satoshi")
            }
            // DASH, DASHt
            coin.type is CoinType.Dash -> {
                val providerDASH = dataProviderManager.dash(baseProvider.name)

                provider = providerDASH
                adapter = FullTransactionBitcoinAdapter(providerDASH, coin, "duff")
            }
            // BNB
            coin.type is CoinType.Binance -> {
                val providerBinance = dataProviderManager.binance(baseProvider.name)

                provider = providerBinance
                adapter = FullTransactionBinanceAdapter(providerBinance, coin)
            }
            //EOS
            coin.type is CoinType.Eos -> {
                val providerEos = dataProviderManager.eos(baseProvider.name)

                provider = providerEos
                adapter = FullTransactionEosAdapter(providerEos, coin)
            }
            // ETH, ETHt
            else -> {
                val providerETH = dataProviderManager.ethereum(baseProvider.name)

                provider = providerETH
                adapter = FullTransactionEthereumAdapter(providerETH, coin)
            }
        }

        return FullTransactionInfoProvider(networkManager, adapter, provider)
    }
}
